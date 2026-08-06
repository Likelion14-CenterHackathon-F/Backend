package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;

import java.time.Instant;

public record JoinConsultationRes(
        Long appointmentId,
        Long sessionId,
        String appId,
        String rtcChannelName,
        String agoraUid,
        String rtcToken,
        Instant tokenExpiresAt,
        ParticipantRole role,
        String userLanguage,
        String peerLanguage,
        String sttPublisherUid,
        int recommendedDurationSeconds,
        Instant forceEndAt
) {
}
