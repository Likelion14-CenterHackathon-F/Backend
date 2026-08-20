package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.centerton.centerton.domain.consultationsummary.client.GeminiClient;
import com.centerton.centerton.domain.consultationsummary.entity.enums.InstructionIcon;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiSummaryServiceTest {

    @Test
    void deserializesObjectInstructionsFromGeminiResponse() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        when(geminiClient.generate(anyString())).thenReturn("""
                {
                  "summary": "상담 요약",
                  "patientConsultationDetails": "환자 상담 내용",
                  "instructions": [
                    {
                      "content": "소독액 도포\\n소독액을 꼼꼼히 발라주세요.",
                      "icon": 5
                    }
                  ]
                }
                """);

        GeminiSummaryService service = new GeminiSummaryService(
                geminiClient,
                new ObjectMapper()
        );
        TranscriptSegment segment = TranscriptSegment.create(
                null,
                1,
                ParticipantRole.MEDICAL_STAFF,
                1,
                "ko",
                "소독액을 꼼꼼히 발라주세요.",
                null,
                null,
                1L,
                1L,
                1_000
        );

        GeminiSummaryService.SummaryResult result = service.summarize(
                List.of(segment)
        );

        assertEquals("상담 요약", result.summary());
        assertEquals("환자 상담 내용", result.patientConsultationDetails());
        assertEquals(1, result.instructions().size());
        assertEquals(
                "소독액 도포\n소독액을 꼼꼼히 발라주세요.",
                result.instructions().getFirst().content()
        );
        assertEquals(
                InstructionIcon.MEDICATION,
                result.instructions().getFirst().icon()
        );
    }
}
