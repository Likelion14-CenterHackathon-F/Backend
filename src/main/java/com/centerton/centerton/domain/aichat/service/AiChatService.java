package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.request.AiChatSymptomInquiryReq;
import com.centerton.centerton.domain.aichat.dto.response.AiChatDownloadImage;
import com.centerton.centerton.domain.aichat.dto.response.AiChatSymptomInquiryRes;
import com.centerton.centerton.domain.aichat.entity.AiChatImageAttachment;
import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.AiChatRoom;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.repository.AiChatImageAttachmentRepository;
import com.centerton.centerton.domain.aichat.repository.AiChatMessageRepository;
import com.centerton.centerton.domain.aichat.repository.AiChatRoomRepository;
import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.domain.aichat.storage.StoredAiChatImage;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.exception.PatientErrorCode;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_QUESTION_LENGTH = 1000;

    private final PatientRepository patientRepository;
    private final AiChatRoomRepository chatRoomRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final AiChatImageAttachmentRepository imageAttachmentRepository;
    private final AiChatImageStorage imageStorage;
    private final AiChatAnswerService answerService;

    @Transactional
    public AiChatSymptomInquiryRes createSymptomInquiry(
            Long patientId,
            AiChatSymptomInquiryReq request
    ) {
        String question = normalizeQuestion(request.getQuestion());
        StoredAiChatImage storedImage = null;

        try {
            if (hasImage(request.getImage())) {
                storedImage = imageStorage.store(request.getImage());
            }

            Patient patient = getPatient(patientId);
            LocalDateTime userSentAt = nowUtc();
            AiChatRoom chatRoom = getOrCreateChatRoom(
                    patient,
                    request.getRoomId(),
                    question,
                    userSentAt
            );

            List<AiChatAnswerMessage> previousMessages = toAnswerMessages(
                    chatRoom.getMessages()
            );

            AiChatMessage userMessage = chatRoom.addUserMessage(
                    question,
                    userSentAt
            );
            attachImageIfPresent(userMessage, storedImage);
            chatMessageRepository.save(userMessage);

            String analysisImageUrl = resolveAnalysisImageUrl(storedImage);
            String answer = answerService.generateAnswer(new AiChatAnswerRequest(
                    question,
                    analysisImageUrl,
                    previousMessages
            ));

            AiChatMessage assistantMessage = chatRoom.addAssistantMessage(
                    answer,
                    nowUtc()
            );
            chatMessageRepository.save(assistantMessage);

            return AiChatSymptomInquiryRes.of(
                    chatRoom,
                    userMessage,
                    assistantMessage
            );
        } catch (RuntimeException exception) {
            deleteStoredImageQuietly(storedImage);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AiChatDownloadImage getImage(
            Long patientId,
            String storedFileName
    ) {
        String imageUrl = imageStorage.resolveDisplayImageUrl(storedFileName);
        AiChatImageAttachment imageAttachment = imageAttachmentRepository
                .findAccessibleImage(imageUrl, patientId)
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.IMAGE_NOT_FOUND
                ));

        return new AiChatDownloadImage(
                imageAttachment.getStoredFileName(),
                imageAttachment.getContentType(),
                imageStorage.load(imageAttachment.getStoredFileName())
        );
    }

    private AiChatRoom getOrCreateChatRoom(
            Patient patient,
            Long roomId,
            String question,
            LocalDateTime now
    ) {
        if (roomId == null) {
            AiChatRoom chatRoom = AiChatRoom.create(
                    patient,
                    question,
                    now
            );
            return chatRoomRepository.save(chatRoom);
        }

        return chatRoomRepository
                .findByIdAndPatientIdForUpdate(roomId, patient.getId())
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.CHAT_ROOM_NOT_FOUND
                ));
    }

    private Patient getPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new BaseException(
                        PatientErrorCode.PATIENT_NOT_FOUND
                ));
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

    private void attachImageIfPresent(
            AiChatMessage userMessage,
            StoredAiChatImage storedImage
    ) {
        if (storedImage == null) {
            return;
        }

        userMessage.attachImage(
                storedImage.storedFileName(),
                storedImage.imageUrl(),
                storedImage.originalFileName(),
                storedImage.contentType(),
                storedImage.sizeBytes()
        );
    }

    private String resolveAnalysisImageUrl(StoredAiChatImage storedImage) {
        if (storedImage == null) {
            return null;
        }

        return imageStorage.resolveAnalysisImageUrl(
                storedImage.storedFileName(),
                storedImage.contentType()
        );
    }

    private List<AiChatAnswerMessage> toAnswerMessages(List<AiChatMessage> messages) {
        return messages.stream()
                .map(message -> new AiChatAnswerMessage(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();
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
