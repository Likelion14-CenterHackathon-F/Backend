package com.centerton.centerton.domain.consultationsummary.client;

import com.centerton.centerton.domain.consultationsummary.config.DeepLProperties;
import com.centerton.centerton.domain.consultationsummary.dto.SummaryLanguage;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeepLTranslationClient {

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
            SummaryLanguage targetLanguage
    ) {
        if (targetLanguage.isKorean() || koreanTexts.isEmpty()) {
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
                "KO",
                targetLanguage.getDeepLTargetCode(),
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
                throw new IllegalStateException("DeepL 번역 개수가 요청과 일치하지 않습니다.");
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
            throw new BaseException(
                    ConsultationSummaryErrorCode.DEEPL_TRANSLATION_FAILED
            );
        }
    }

    private void validateConfiguration() {
        if (properties.getAuthKey() == null || properties.getAuthKey().isBlank()) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.DEEPL_CONFIGURATION_MISSING
            );
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
