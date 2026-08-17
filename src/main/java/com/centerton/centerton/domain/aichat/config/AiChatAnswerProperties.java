package com.centerton.centerton.domain.aichat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-chat.answer")
public class AiChatAnswerProperties {

    /** {@code openai} 직접 호출은 근거 없이 답하므로 명시적으로 지정해야만 쓰인다. */
    private String provider = "rag-service";
}
