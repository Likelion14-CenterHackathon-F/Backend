package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.AiChatRoom;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.repository.AiChatMessageRepository;
import com.centerton.centerton.domain.aichat.repository.AiChatRoomRepository;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.exception.PatientErrorCode;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatTransactionService {

    private final PatientRepository patientRepository;
    private final AiChatRoomRepository chatRoomRepository;
    private final AiChatMessageRepository chatMessageRepository;

    @Transactional
    public SavedAiChatUserMessage saveUserMessage(
            Long patientId,
            Long roomId,
            String question,
            String imageUrl,
            LocalDateTime sentAt
    ) {
        Patient patient = getPatient(patientId);
        AiChatRoom chatRoom = getOrCreateChatRoom(
                patient,
                roomId,
                question,
                sentAt
        );
        List<AiChatAnswerMessage> previousMessages = toAnswerMessages(
                chatRoom.getMessages()
        );

        AiChatMessage userMessage = chatRoom.addUserMessage(
                question,
                sentAt
        );

        if (imageUrl != null && !imageUrl.isBlank()) {
            userMessage.attachImage(imageUrl);
        }

        chatMessageRepository.save(userMessage);

        return new SavedAiChatUserMessage(
                chatRoom,
                userMessage,
                previousMessages
        );
    }

    @Transactional
    public AiChatMessage saveAssistantMessage(
            Long patientId,
            Long roomId,
            String answer,
            LocalDateTime sentAt
    ) {
        AiChatRoom chatRoom = chatRoomRepository
                .findByIdAndPatientIdForUpdate(roomId, patientId)
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.CHAT_ROOM_NOT_FOUND
                ));
        AiChatMessage assistantMessage = chatRoom.addAssistantMessage(
                answer,
                sentAt
        );

        return chatMessageRepository.save(assistantMessage);
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

    private List<AiChatAnswerMessage> toAnswerMessages(List<AiChatMessage> messages) {
        return messages.stream()
                .map(message -> new AiChatAnswerMessage(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();
    }
}
