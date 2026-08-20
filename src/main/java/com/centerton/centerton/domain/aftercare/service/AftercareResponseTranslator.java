package com.centerton.centerton.domain.aftercare.service;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareDashboardDetailRes;
import com.centerton.centerton.domain.aftercare.dto.response.AftercareHomeRes;
import com.centerton.centerton.domain.aftercare.exception.AftercareErrorCode;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.translation.DeepLConfigurationException;
import com.centerton.centerton.global.translation.DeepLTranslationClient;
import com.centerton.centerton.global.translation.DeepLTranslationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AftercareResponseTranslator {

    private final DeepLTranslationClient translationClient;

    public AftercareHomeRes translateHome(
            AftercareHomeRes response,
            Language language
    ) {
        AftercareHomeRes.ProcedureSummary procedure = response.procedure();
        List<String> translatedTexts = translateTexts(
                List.of(response.patientName(), procedure.procedureName()),
                language
        );

        return new AftercareHomeRes(
                response.caseId(),
                translatedTexts.get(0),
                response.aftercareProgress(),
                new AftercareHomeRes.ProcedureSummary(
                        translatedTexts.get(1),
                        procedure.procedureDate()
                ),
                response.consultationAppointment()
        );
    }

    public AftercareDashboardDetailRes translateDashboardDetail(
            AftercareDashboardDetailRes response,
            Language language
    ) {
        List<String> texts = new ArrayList<>();
        texts.add(response.caseStatus().procedureName());

        for (AftercareDashboardDetailRes.RecoveryGuide guide :
                response.recoveryGuides()) {

            texts.add(guide.recoveryStage());
            texts.addAll(guide.guideItems());
        }

        texts.addAll(response.redFlags().items());

        List<String> translatedTexts = translateTexts(texts, language);

        int textIndex = 0;
        AftercareDashboardDetailRes.CaseStatus translatedCaseStatus =
                new AftercareDashboardDetailRes.CaseStatus(
                        translatedTexts.get(textIndex++),
                        response.caseStatus().procedureDate(),
                        response.caseStatus().currentDay(),
                        response.caseStatus().totalCareDays()
                );

        List<AftercareDashboardDetailRes.RecoveryGuide> translatedGuides =
                new ArrayList<>();

        for (AftercareDashboardDetailRes.RecoveryGuide guide :
                response.recoveryGuides()) {

            String translatedRecoveryStage = translatedTexts.get(textIndex++);
            List<String> translatedGuideItems = new ArrayList<>();

            for (int index = 0; index < guide.guideItems().size(); index++) {
                translatedGuideItems.add(translatedTexts.get(textIndex++));
            }

            translatedGuides.add(
                    new AftercareDashboardDetailRes.RecoveryGuide(
                            guide.stageGuideId(),
                            translatedRecoveryStage,
                            guide.startDay(),
                            guide.endDay(),
                            List.copyOf(translatedGuideItems),
                            guide.status()
                    )
            );
        }

        List<String> translatedRedFlags = new ArrayList<>();
        for (int index = 0; index < response.redFlags().items().size(); index++) {
            translatedRedFlags.add(translatedTexts.get(textIndex++));
        }

        return new AftercareDashboardDetailRes(
                response.caseId(),
                translatedCaseStatus,
                List.copyOf(translatedGuides),
                new AftercareDashboardDetailRes.RedFlags(
                        List.copyOf(translatedRedFlags)
                )
        );
    }

    private List<String> translateTexts(
            List<String> koreanTexts,
            Language language
    ) {
        try {
            return translationClient.translateKoreanTexts(
                    koreanTexts,
                    language == null ? null : language.name()
            );
        } catch (DeepLConfigurationException | DeepLTranslationException exception) {
            throw new BaseException(AftercareErrorCode.TRANSLATION_FAILED);
        }
    }
}
