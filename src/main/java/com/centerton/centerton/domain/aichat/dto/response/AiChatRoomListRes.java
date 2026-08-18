package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatRoom;
import com.centerton.centerton.global.util.UtcDateTimeUtils;

import java.time.OffsetDateTime;

public record AiChatRoomListRes(
        Long roomId,
        String roomTitle,
        OffsetDateTime lastMessageAt
) {

    public static AiChatRoomListRes from(AiChatRoom chatRoom) {
        return new AiChatRoomListRes(
                chatRoom.getChatRoomId(),
                chatRoom.getTitle(),
                UtcDateTimeUtils.toUtcOffset(chatRoom.getLastMessageAt())
        );
    }
}
