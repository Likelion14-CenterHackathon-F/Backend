package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.ParticipantRole;

import java.time.Instant;

public record JoinConsultationRes(
        Long consultationId,
        Long sessionId,
        String appId,
        String channelName,
        String uid,
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
