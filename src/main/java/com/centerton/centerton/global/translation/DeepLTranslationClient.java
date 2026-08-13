package com.centerton.centerton.global.translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeepLTranslationClient {

    private static final String KOREAN_SOURCE_CODE = "KO";

    private final RestClient restClient;
    private final DeepLProperties properties;

    public DeepLTranslationClient(
            @Qualifier("deepLRestClient") RestClient restClient,
            DeepLProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<String> translateKoreanTexts(
            List<String> koreanTexts,
            String targetLanguageCode
    ) {
        if (isKorean(targetLanguageCode) || koreanTexts.isEmpty()) {
            return List.copyOf(koreanTexts);
        }

        List<Integer> translatedIndexes = new ArrayList<>();
        List<String> translatableTexts = new ArrayList<>();
        for (int index = 0; index < koreanTexts.size(); index++) {
            String text = koreanTexts.get(index);
            if (text != null && !text.isBlank()) {
                translatedIndexes.add(index);
                translatableTexts.add(text);
            }
        }
        if (translatableTexts.isEmpty()) {
            return List.copyOf(koreanTexts);
        }

        validateConfiguration();
        DeepLTranslateRequest request = new DeepLTranslateRequest(
                translatableTexts,
                KOREAN_SOURCE_CODE,
                targetLanguageCode,
                true
        );

        try {
            DeepLTranslateResponse response = restClient.post()
                    .uri("/v2/translate")
                    .header("Authorization", "DeepL-Auth-Key " + properties.getAuthKey())
                    .body(request)
                    .retrieve()
                    .body(DeepLTranslateResponse.class);

            if (response == null
                    || response.translations() == null
                    || response.translations().size() != translatableTexts.size()) {
                throw new IllegalStateException("DeepL translation count does not match request count.");
            }

            List<String> translatedTexts = new ArrayList<>(koreanTexts);
            for (int index = 0; index < translatedIndexes.size(); index++) {
                translatedTexts.set(
                        translatedIndexes.get(index),
                        response.translations().get(index).text()
                );
            }
            return List.copyOf(translatedTexts);
        } catch (RestClientException | IllegalStateException exception) {
            throw new DeepLTranslationException();
        }
    }

    private boolean isKorean(String targetLanguageCode) {
        return targetLanguageCode == null
                || targetLanguageCode.isBlank()
                || KOREAN_SOURCE_CODE.equalsIgnoreCase(targetLanguageCode.trim());
    }

    private void validateConfiguration() {
        if (properties.getAuthKey() == null || properties.getAuthKey().isBlank()) {
            throw new DeepLConfigurationException();
        }
    }

    private record DeepLTranslateRequest(
            List<String> text,
            @JsonProperty("source_lang") String sourceLanguage,
            @JsonProperty("target_lang") String targetLanguage,
            @JsonProperty("preserve_formatting") boolean preserveFormatting
    ) {
    }

    private record DeepLTranslateResponse(List<DeepLTranslation> translations) {
    }

    private record DeepLTranslation(String text) {
    }
}
