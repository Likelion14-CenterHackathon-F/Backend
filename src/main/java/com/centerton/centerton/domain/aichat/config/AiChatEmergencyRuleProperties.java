package com.centerton.centerton.domain.aichat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-chat.emergency-rules")
public class AiChatEmergencyRuleProperties {

    private String path = "classpath:rag/emergency_rules.json";
}
