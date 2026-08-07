package com.centerton.centerton.domain.consultation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "agora")
public class AgoraProperties {

    private String appId;
    private String appCertificate;
    private String customerId;
    private String customerSecret;
    private int tokenExpirationSeconds = 3600;
    private Stt stt = new Stt();

    @Getter
    @Setter
    public static class Stt {

        private String baseUrl = "https://api.agora.io/api/speech-to-text/v1";
        private int subBotUid = 900001;
        private int pubBotUid = 900002;
        private int maxIdleTimeSeconds = 120;
        private List<String> keywords = new ArrayList<>();
    }
}
