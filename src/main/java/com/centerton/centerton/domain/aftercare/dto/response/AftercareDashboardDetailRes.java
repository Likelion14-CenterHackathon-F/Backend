package com.centerton.centerton.domain.aftercare.dto.response;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.entity.ProcedureRecord;
import com.centerton.centerton.domain.aftercare.entity.RecoveryStageGuide;
import com.centerton.centerton.domain.aftercare.entity.enums.RecoveryStage;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public record AftercareDashboardDetailRes(
        Long caseId,
        CaseStatus caseStatus,
        List<RecoveryGuide> recoveryGuides,
        RedFlags redFlags
) {

    public static AftercareDashboardDetailRes from(AftercareCase aftercareCase, LocalDate referenceDate) {
        int currentDay = calculateCurrentDay(aftercareCase, referenceDate);
        ProcedureRecord procedureRecord = aftercareCase.getProcedureRecord();

        return new AftercareDashboardDetailRes(
                aftercareCase.getCaseId(),
                CaseStatus.from(aftercareCase, procedureRecord, currentDay),
                aftercareCase.getRecoveryStageGuides().stream()
                        .map(guide -> RecoveryGuide.from(guide, currentDay))
                        .toList(),
                RedFlags.defaultRedFlags()
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
                    toGuideItems(guide.getGuideContent()),
                    guideStatus.name()
            );
        }
    }

    public record RedFlags(List<String> items) {

        private static RedFlags defaultRedFlags() {
            return new RedFlags(
                    List.of(
                            "38.5°C 이상의 발열이 지속되는 경우",
                            "상처 부위에서 비정상적인 출혈 또는 분비물",
                            "심한 통증이 갑자기 악화되는 경우"
                    )
            );
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

    private static int calculateCurrentDay(AftercareCase aftercareCase, LocalDate referenceDate) {
        if (aftercareCase.getAftercareStartDate() == null || referenceDate == null) {
            return 1;
        }
        return Math.max((int) ChronoUnit.DAYS.between(aftercareCase.getAftercareStartDate(), referenceDate) + 1, 1);
    }

    private static List<String> toGuideItems(String guideContent) {
        if (guideContent == null || guideContent.isBlank()) {
            return List.of();
        }

        return Arrays.stream(guideContent.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.startsWith("•") ? line.substring(1).trim() : line)
                .toList();
    }
}
