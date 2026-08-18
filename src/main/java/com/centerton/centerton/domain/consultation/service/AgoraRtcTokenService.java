package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.consultation.config.AgoraProperties;
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.util.UtcDateTimeUtils;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AgoraRtcTokenService {

    private final AgoraProperties agoraProperties;

    public AgoraRtcTokenService(AgoraProperties agoraProperties) {
        this.agoraProperties = agoraProperties;
    }

    public IssuedRtcToken issuePublisherToken(
            String channelName,
            int agoraUid
    ) {
        validateConfiguration();

        int expirationSeconds = agoraProperties.getTokenExpirationSeconds();

        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        String token = tokenBuilder.buildTokenWithUid(
                agoraProperties.getAppId(),
                agoraProperties.getAppCertificate(),
                channelName,
                agoraUid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                expirationSeconds,
                expirationSeconds
        );

        if (token == null || token.isBlank()) {
            throw new BaseException(
                    ConsultationErrorCode.AGORA_TOKEN_ISSUE_FAILED
            );
        }

        return new IssuedRtcToken(
                token,
                UtcDateTimeUtils.truncateToSeconds(
                        Instant.now().plusSeconds(expirationSeconds)
                )
        );
    }

    private void validateConfiguration() {
        if (isBlank(agoraProperties.getAppId())
                || isBlank(agoraProperties.getAppCertificate())) {
            throw new BaseException(
                    ConsultationErrorCode.AGORA_CONFIGURATION_MISSING
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record IssuedRtcToken(
            String token,
            Instant expiresAt
    ) {
    }
}
