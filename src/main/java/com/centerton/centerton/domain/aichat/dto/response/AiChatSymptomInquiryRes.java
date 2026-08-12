package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.AiChatRoom;

import java.util.List;

public record AiChatSymptomInquiryRes(
        Long roomId,
        String roomTitle,
        List<AiChatMessageRes> messages
) {

    public static AiChatSymptomInquiryRes of(
            AiChatRoom chatRoom,
            AiChatMessage userMessage,
            AiChatMessage assistantMessage
    ) {
        return new AiChatSymptomInquiryRes(
                chatRoom.getChatRoomId(),
                chatRoom.getTitle(),
                List.of(
                        AiChatMessageRes.from(userMessage),
                        AiChatMessageRes.from(assistantMessage)
                )
        );
    }
}
