package com.centerton.centerton.domain.aftercare.dto.response;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.entity.ProcedureRecord;
import com.centerton.centerton.domain.aftercare.entity.RecoveryStageGuide;
import com.centerton.centerton.domain.aftercare.entity.enums.RecoveryStage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record AftercareDashboardDetailRes(
        Long caseId,
        CaseStatus caseStatus,
        List<RecoveryGuide> recoveryGuides,
        RedFlags redFlags
) {

    public static AftercareDashboardDetailRes from(AftercareCase aftercareCase, LocalDate referenceDate) {
        int currentDay = aftercareCase.calculateCurrentDay(referenceDate);
        ProcedureRecord procedureRecord = aftercareCase.getProcedureRecord();

        return new AftercareDashboardDetailRes(
                aftercareCase.getCaseId(),
                CaseStatus.from(aftercareCase, procedureRecord, currentDay),
                aftercareCase.getRecoveryStageGuides().stream()
                        .map(guide -> RecoveryGuide.from(guide, currentDay))
                        .toList(),
                RedFlags.from(aftercareCase.getRedFlagSigns())
        );
    }

    public record CaseStatus(
            String procedureName,
            LocalDate procedureDate,
            Integer currentDay,
            Integer totalCareDays
    ) {

        private static CaseStatus from(AftercareCase aftercareCase, ProcedureRecord procedureRecord, int currentDay) {
            Integer totalCareDays = aftercareCase.getTotalCareDays();

            return new CaseStatus(
                    procedureRecord.getProcedureName(),
                    procedureRecord.getProcedureDate(),
                    currentDay,
                    totalCareDays
            );
        }
    }

    public record RecoveryGuide(
            Long stageGuideId,
            RecoveryStage recoveryStage,
            Integer startDay,
            Integer endDay,
            List<String> guideItems,
            String status
    ) {

        private static RecoveryGuide from(RecoveryStageGuide guide, int currentDay) {
            GuideStatus guideStatus = GuideStatus.from(guide, currentDay);

            return new RecoveryGuide(
                    guide.getStageGuideId(),
                    guide.getRecoveryStage(),
                    guide.getStartDay(),
                    guide.getEndDay(),
                    toLineItems(guide.getGuideContent()),
                    guideStatus.name()
            );
        }
    }

    public record RedFlags(List<String> items) {

        private static RedFlags from(String redFlagSigns) {
            return new RedFlags(toLineItems(redFlagSigns));
        }
    }

    private enum GuideStatus {
        PAST,
        CURRENT,
        UPCOMING;

        private static GuideStatus from(RecoveryStageGuide guide, int currentDay) {
            if (guide.includes(currentDay)) {
                return CURRENT;
            }
            if (currentDay < guide.getStartDay()) {
                return UPCOMING;
            }
            return PAST;
        }
    }

    private static List<String> toLineItems(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.startsWith("•") ? line.substring(1).trim() : line)
                .toList();
    }
}
