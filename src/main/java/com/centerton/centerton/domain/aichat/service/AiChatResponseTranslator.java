package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.response.AiChatMessageRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomListRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomMessagesRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatSymptomInquiryRes;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.translation.DeepLConfigurationException;
import com.centerton.centerton.global.translation.DeepLTranslationClient;
import com.centerton.centerton.global.translation.DeepLTranslationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiChatResponseTranslator {

    private final DeepLTranslationClient translationClient;

    public AiChatSymptomInquiryRes translateSymptomInquiry(
            AiChatSymptomInquiryRes response,
            Language language
    ) {
        TranslatedRoomMessages translated = translateRoomMessages(
                response.roomTitle(),
                response.messages(),
                language
        );

        return new AiChatSymptomInquiryRes(
                response.roomId(),
                translated.roomTitle(),
                translated.messages()
        );
    }

    public List<AiChatRoomListRes> translateChatRooms(
            List<AiChatRoomListRes> responses,
            Language language
    ) {
        if (responses.isEmpty()) {
            return List.of();
        }

        List<String> roomTitles = responses.stream()
                .map(AiChatRoomListRes::roomTitle)
                .toList();
        List<String> translatedRoomTitles = translateTexts(roomTitles, language);

        List<AiChatRoomListRes> translatedResponses = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            AiChatRoomListRes response = responses.get(index);
            translatedResponses.add(new AiChatRoomListRes(
                    response.roomId(),
                    translatedRoomTitles.get(index),
                    response.lastMessageAt()
            ));
        }

        return List.copyOf(translatedResponses);
    }

    public AiChatRoomMessagesRes translateChatRoomMessages(
            AiChatRoomMessagesRes response,
            Language language
    ) {
        TranslatedRoomMessages translated = translateRoomMessages(
                response.roomTitle(),
                response.messages(),
                language
        );

        return new AiChatRoomMessagesRes(
                response.roomId(),
                translated.roomTitle(),
                translated.messages()
        );
    }

    private TranslatedRoomMessages translateRoomMessages(
            String roomTitle,
            List<AiChatMessageRes> messages,
            Language language
    ) {
        List<String> texts = new ArrayList<>();
        texts.add(roomTitle);
        messages.stream()
                .map(AiChatMessageRes::content)
                .forEach(texts::add);

        List<String> translatedTexts = translateTexts(texts, language);

        List<AiChatMessageRes> translatedMessages = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            translatedMessages.add(
                    translateMessage(
                            messages.get(index),
                            translatedTexts.get(index + 1)
                    )
            );
        }

        return new TranslatedRoomMessages(
                translatedTexts.getFirst(),
                List.copyOf(translatedMessages)
        );
    }

    private AiChatMessageRes translateMessage(
            AiChatMessageRes message,
            String translatedContent
    ) {
        return new AiChatMessageRes(
                message.messageId(),
                message.role(),
                translatedContent,
                message.imageUrl(),
                message.sentAt()
        );
    }

    private List<String> translateTexts(
            List<String> texts,
            Language language
    ) {
        try {
            return translationClient.translateKoreanTextsContainingHangul(
                    texts,
                    language == null ? null : language.name()
            );
        } catch (DeepLConfigurationException | DeepLTranslationException exception) {
            throw new BaseException(AiChatErrorCode.TRANSLATION_FAILED);
        }
    }

    private record TranslatedRoomMessages(
            String roomTitle,
            List<AiChatMessageRes> messages
    ) {
    }
}
