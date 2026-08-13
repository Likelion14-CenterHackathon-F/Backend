package com.centerton.centerton.domain.aichat.service;

import java.util.List;

public record AiChatAnswerRequest(
        String question,
        String analysisImageUrl,
        List<AiChatAnswerMessage> previousMessages
) {

    public boolean hasImage() {
        return analysisImageUrl != null && !analysisImageUrl.isBlank();
    }
}
