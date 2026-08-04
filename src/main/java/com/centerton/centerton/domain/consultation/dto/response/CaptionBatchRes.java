package com.centerton.centerton.domain.consultation.dto.response;

public record CaptionBatchRes(
        int receivedCount,
        int insertedCount,
        int updatedCount
) {
}
