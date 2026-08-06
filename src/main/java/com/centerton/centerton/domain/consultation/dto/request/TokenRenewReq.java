package com.centerton.centerton.domain.consultation.dto.request;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import jakarta.validation.constraints.NotNull;

public record TokenRenewReq(

        @NotNull
        ParticipantRole role
) {
}
