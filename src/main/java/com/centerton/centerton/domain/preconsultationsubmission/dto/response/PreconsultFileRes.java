package com.centerton.centerton.domain.preconsultationsubmission.dto.response;

import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;

public record PreconsultFileRes(
        Long fileId,
        String fileUrl
) {

    public static PreconsultFileRes from(FileAsset fileAsset) {
        return new PreconsultFileRes(
                fileAsset.getFileId(),
                fileAsset.getFileUrl()
        );
    }
}
