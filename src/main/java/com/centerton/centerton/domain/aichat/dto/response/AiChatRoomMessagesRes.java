package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatRoom;

import java.util.List;

public record AiChatRoomMessagesRes(
        Long roomId,
        String roomTitle,
        List<AiChatMessageRes> messages
) {

    public static AiChatRoomMessagesRes from(AiChatRoom chatRoom) {
        return new AiChatRoomMessagesRes(
                chatRoom.getChatRoomId(),
                chatRoom.getTitle(),
                chatRoom.getMessages()
                        .stream()
                        .map(AiChatMessageRes::from)
                        .toList()
        );
    }
}
