package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatRoom;

import java.time.LocalDateTime;

public record AiChatRoomListRes(
        Long roomId,
        String title,
        LocalDateTime lastMessageAt
) {

    public static AiChatRoomListRes from(AiChatRoom chatRoom) {
        return new AiChatRoomListRes(
                chatRoom.getChatRoomId(),
                chatRoom.getTitle(),
                chatRoom.getLastMessageAt()
        );
    }
}
