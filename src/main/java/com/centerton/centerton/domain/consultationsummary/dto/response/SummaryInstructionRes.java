package com.centerton.centerton.domain.consultationsummary.dto.response;

import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;
import com.centerton.centerton.global.util.UtcDateTimeUtils;

import java.time.OffsetDateTime;

public record SummaryInstructionRes(
        Long instructionId,
        String title,
        String content,
        Integer sortOrder,
        Boolean patientCompleted,
        OffsetDateTime completedAt
) {

    private static final String DEFAULT_TITLE = "상담 안내";

    public static SummaryInstructionRes from(
            SummaryInstruction instruction
    ) {
        ParsedInstruction parsed =
                parseInstruction(instruction.getContent());

        return new SummaryInstructionRes(
                instruction.getInstructionId(),
                parsed.title(),
                parsed.content(),
                instruction.getSortOrder(),
                instruction.getPatientCompleted(),
                UtcDateTimeUtils.toUtcOffset(
                        instruction.getCompletedAt()
                )
        );
    }

    public static SummaryInstructionRes translated(
            SummaryInstruction instruction,
            String translatedContent
    ) {
        ParsedInstruction parsed =
                parseInstruction(translatedContent);

        return new SummaryInstructionRes(
                instruction.getInstructionId(),
                parsed.title(),
                parsed.content(),
                instruction.getSortOrder(),
                instruction.getPatientCompleted(),
                UtcDateTimeUtils.toUtcOffset(
                        instruction.getCompletedAt()
                )
        );
    }

    private static ParsedInstruction parseInstruction(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return new ParsedInstruction(
                    DEFAULT_TITLE,
                    ""
            );
        }

        String normalized =
                value
                        .replace("\r\n", "\n")
                        .replace('\r', '\n')
                        .trim();

        int separatorIndex =
                normalized.indexOf('\n');

        /*
         * 기존에 생성되어 있던 상담 요약처럼
         * 제목 구분이 없는 데이터도 깨지지 않도록 처리합니다.
         */
        if (separatorIndex < 0) {
            return new ParsedInstruction(
                    DEFAULT_TITLE,
                    normalized
            );
        }

        String title =
                normalized
                        .substring(0, separatorIndex)
                        .trim();

        String content =
                normalized
                        .substring(separatorIndex + 1)
                        .trim();

        if (title.isBlank()) {
            title = DEFAULT_TITLE;
        }

        /*
         * Gemini가 잘못된 형식으로 제목만 반환한 경우에도
         * 기존 content를 잃지 않도록 방어합니다.
         */
        if (content.isBlank()) {
            return new ParsedInstruction(
                    DEFAULT_TITLE,
                    normalized
            );
        }

        return new ParsedInstruction(
                title,
                content
        );
    }

    private record ParsedInstruction(
            String title,
            String content
    ) {
    }
}