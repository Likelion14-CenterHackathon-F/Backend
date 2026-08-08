package com.centerton.centerton.domain.consultation.dto.request;

import com.centerton.centerton.domain.consultation.entity.enums.ConsultationLanguage;
import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record JoinConsultationReq(

        @NotNull
        ParticipantRole role,

        @NotNull
        @Min(1)
        Integer agoraUid,

        @NotNull
        ConsultationLanguage userLanguage
) {
}