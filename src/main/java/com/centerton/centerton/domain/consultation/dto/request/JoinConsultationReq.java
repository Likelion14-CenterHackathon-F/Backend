package com.centerton.centerton.domain.consultation.dto.request;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinConsultationReq(
        @NotNull
        ParticipantRole role,

        @NotNull
        @Min(1)
        Integer agoraUid,

        @NotBlank
        String userLanguage
) {
}
