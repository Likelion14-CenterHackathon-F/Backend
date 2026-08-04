package com.centerton.centerton.domain.consultation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CaptionBatchReq(

        @NotNull
        Long sessionId,

        @NotEmpty
        List<@Valid CaptionItemReq> captions
) {
}
