package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.request.AiChatSymptomInquiryReq;
import com.centerton.centerton.domain.aichat.dto.response.AiChatDownloadImage;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomListRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomMessagesRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatSymptomInquiryRes;
import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.safety.AiChatEmergencyRuleService;
import com.centerton.centerton.domain.aichat.safety.EmergencyRuleMatch;
import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.domain.aichat.storage.StoredAiChatImage;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_QUESTION_LENGTH = 1000;

    private final AiChatImageStorage imageStorage;
    private final AiChatAnswerService answerService;
    private final AiChatTransactionService transactionService;
    private final AiChatQueryService queryService;
    private final AiChatResponseTranslator responseTranslator;
    private final AiChatEmergencyRuleService emergencyRuleService;

    public AiChatSymptomInquiryRes createSymptomInquiry(
            Long patientId,
            Language language,
            AiChatSymptomInquiryReq request
    ) {
        String question = normalizeQuestion(request.getQuestion());
        StoredAiChatImage storedImage = null;
        boolean userMessageSaved = false;

        try {
            if (hasImage(request.getImage())) {
                storedImage = imageStorage.store(request.getImage());
            }

            LocalDateTime userSentAt = nowUtc();
            SavedAiChatUserMessage savedUserMessage = transactionService.saveUserMessage(
                    patientId,
                    request.getRoomId(),
                    question,
                    resolveDisplayImageUrl(storedImage),
                    userSentAt
            );
            userMessageSaved = true;

            String analysisImageUrl = resolveAnalysisImageUrl(storedImage);
            String answer = createAnswer(
                    question,
                    analysisImageUrl,
                    savedUserMessage.previousMessages()
            );

            AiChatMessage assistantMessage = transactionService.saveAssistantMessage(
                    patientId,
                    savedUserMessage.chatRoom().getChatRoomId(),
                    answer,
                    nowUtc()
            );

            return responseTranslator.translateSymptomInquiry(
                    AiChatSymptomInquiryRes.of(
                            savedUserMessage.chatRoom(),
                            savedUserMessage.userMessage(),
                            assistantMessage
                    ),
                    language
            );
        } catch (RuntimeException exception) {
            if (!userMessageSaved) {
                deleteStoredImageQuietly(storedImage);
            }
            throw exception;
        }
    }

    private String createAnswer(
            String question,
            String analysisImageUrl,
            List<AiChatAnswerMessage> previousMessages
    ) {
        Optional<EmergencyRuleMatch> emergencyRuleMatch =
                emergencyRuleService.findMatch(question);

        if (emergencyRuleMatch.isPresent()) {
            EmergencyRuleMatch match = emergencyRuleMatch.get();
            log.warn(
                    "AI 채팅 응급 하드스톱 룰 매칭. ruleIds={}, signals={}",
                    match.ruleIds(),
                    match.matchedSignals()
            );
            return match.frontendMessage();
        }

        return answerService.generateAnswer(new AiChatAnswerRequest(
                question,
                analysisImageUrl,
                previousMessages
        ));
    }

    public List<AiChatRoomListRes> getChatRooms(
            Long patientId,
            Language language
    ) {
        return responseTranslator.translateChatRooms(
                queryService.getChatRooms(patientId),
                language
        );
    }

    public AiChatRoomMessagesRes getChatRoomMessages(
            Long patientId,
            Language language,
            Long roomId
    ) {
        return responseTranslator.translateChatRoomMessages(
                queryService.getChatRoomMessages(patientId, roomId),
                language
        );
    }

    public AiChatDownloadImage getImage(
            Long patientId,
            String storedFileName
    ) {
        return queryService.getImage(patientId, storedFileName);
    }

    private String normalizeQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new BaseException(AiChatErrorCode.QUESTION_REQUIRED);
        }

        String normalized = question.strip();
        if (normalized.length() > MAX_QUESTION_LENGTH) {
            throw new BaseException(AiChatErrorCode.QUESTION_TOO_LONG);
        }
        return normalized;
    }

    private boolean hasImage(MultipartFile image) {
        if (image == null) {
            return false;
        }

        if (image.isEmpty()) {
            throw new BaseException(AiChatErrorCode.IMAGE_EMPTY);
        }

        return true;
    }

    private String resolveDisplayImageUrl(StoredAiChatImage storedImage) {
        if (storedImage == null) {
            return null;
        }

        return storedImage.imageUrl();
    }

    private String resolveAnalysisImageUrl(StoredAiChatImage storedImage) {
        if (storedImage == null) {
            return null;
        }

        return imageStorage.resolveAnalysisImageUrl(storedImage.storedFileName());
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void deleteStoredImageQuietly(StoredAiChatImage storedImage) {
        if (storedImage == null) {
            return;
        }

        try {
            imageStorage.delete(storedImage.storedFileName());
        } catch (RuntimeException exception) {
            log.error(
                    "DB에 반영되지 않은 AI 채팅 이미지를 삭제하지 못했습니다. storedFileName={}",
                    storedImage.storedFileName(),
                    exception
            );
        }
    }
}
