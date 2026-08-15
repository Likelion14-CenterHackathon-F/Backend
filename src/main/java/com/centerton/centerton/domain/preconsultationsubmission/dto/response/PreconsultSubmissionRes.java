package com.centerton.centerton.domain.preconsultationsubmission.dto.response;

import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;

import java.util.List;

public record PreconsultSubmissionRes(
        Long submissionId,
        Long appointmentId,
        SymptomCategory symptomCategory,
        List<SymptomCategory> symptomCategories,
        String symptomNote,
        List<PreconsultFileRes> files
) {

    public static PreconsultSubmissionRes of(
            PreconsultSubmission submission,
            List<FileAsset> fileAssets
    ) {
        return new PreconsultSubmissionRes(
                submission.getSubmissionId(),
                submission.getAppointmentId(),
                submission.getSymptomCategory(),
                submission.getOrderedSymptomCategories(),
                submission.getSymptomNote(),
                fileAssets.stream()
                        .map(PreconsultFileRes::from)
                        .toList()
        );
    }
}
