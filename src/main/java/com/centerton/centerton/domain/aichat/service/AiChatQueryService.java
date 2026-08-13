package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.response.AiChatDownloadImage;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomListRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomMessagesRes;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.repository.AiChatMessageRepository;
import com.centerton.centerton.domain.aichat.repository.AiChatRoomRepository;
import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiChatQueryService {

    private final AiChatRoomRepository chatRoomRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final AiChatImageStorage imageStorage;

    public List<AiChatRoomListRes> getChatRooms(Long patientId) {
        return chatRoomRepository.findAllByPatientIdOrderByLastMessageAtDescChatRoomIdDesc(patientId)
                .stream()
                .map(AiChatRoomListRes::from)
                .toList();
    }

    public AiChatRoomMessagesRes getChatRoomMessages(
            Long patientId,
            Long roomId
    ) {
        return chatRoomRepository.findByChatRoomIdAndPatientId(roomId, patientId)
                .map(AiChatRoomMessagesRes::from)
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.CHAT_ROOM_NOT_FOUND
                ));
    }

    public AiChatDownloadImage getImage(
            Long patientId,
            String storedFileName
    ) {
        String imageUrl = imageStorage.resolveDisplayImageUrl(storedFileName);
        chatMessageRepository.findAccessibleImageMessage(imageUrl, patientId)
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.IMAGE_NOT_FOUND
                ));

        return new AiChatDownloadImage(
                storedFileName,
                imageStorage.resolveContentType(storedFileName),
                imageStorage.load(storedFileName)
        );
    }
}
