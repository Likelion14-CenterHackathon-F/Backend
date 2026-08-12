package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatImageAttachment;

public record AiChatImageAttachmentRes(
        Long imageAttachmentId,
        String imageUrl,
        String originalFileName,
        String contentType
) {

    public static AiChatImageAttachmentRes from(AiChatImageAttachment imageAttachment) {
        if (imageAttachment == null) {
            return null;
        }

        return new AiChatImageAttachmentRes(
                imageAttachment.getImageAttachmentId(),
                imageAttachment.getImageUrl(),
                imageAttachment.getOriginalFileName(),
                imageAttachment.getContentType()
        );
    }
}
