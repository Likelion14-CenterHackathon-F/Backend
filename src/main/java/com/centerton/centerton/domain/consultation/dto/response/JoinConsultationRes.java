package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;

import java.time.Instant;

public record JoinConsultationRes(
        Long appointmentId,
        Long sessionId,
        String agoraAppId,
        String rtcChannelName,
        Integer agoraUid,
        String rtcToken,
        Instant tokenExpiresAt,
        ParticipantRole role,
        String userLanguage,
        String peerLanguage,
        Integer sttPublisherAgoraUid,
        int recommendedDurationSeconds,
        Instant forceEndAt
) {
}
