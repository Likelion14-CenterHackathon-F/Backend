package com.centerton.centerton.domain.aichat.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 프론트는 `messages[].content` 하나만 렌더링하므로 RAG 의 구조화 응답을 문자열 하나로
 * 합쳐야 한다. 합치는 규칙이 라우트마다 다르기 때문에 테스트로 고정한다.
 */
class FastApiRagChatAnswerServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String body) {
        return MAPPER.readTree(body);
    }

    @Test
    @DisplayName("rag_answer 는 화상상담 CTA 문장을 answer 뒤에 붙인다")
    void ragAnswerAppendsRecommendedAction() {
        String content = FastApiRagChatAnswerService.composeContent(json("""
                {
                  "answer": "부기 때문에 코끝이 비대칭처럼 보일 수 있습니다.",
                  "route": "rag_answer",
                  "consultationCta": "video_consult",
                  "recommendedAction": "정확한 상태 확인을 위해 화상 상담 예약을 권장드립니다."
                }
                """));

        assertEquals("""
                부기 때문에 코끝이 비대칭처럼 보일 수 있습니다.

                정확한 상태 확인을 위해 화상 상담 예약을 권장드립니다.""", content);
    }

    @Test
    @DisplayName("answer 가 이미 같은 문장을 담고 있으면 중복해서 붙이지 않는다")
    void doesNotDuplicateRecommendedAction() {
        String content = FastApiRagChatAnswerService.composeContent(json("""
                {
                  "answer": "관찰이 필요합니다. 정확한 상태 확인을 위해 화상 상담 예약을 권장드립니다.",
                  "route": "rag_answer",
                  "recommendedAction": "정확한 상태 확인을 위해 화상 상담 예약을 권장드립니다."
                }
                """));

        assertEquals("관찰이 필요합니다. 정확한 상태 확인을 위해 화상 상담 예약을 권장드립니다.", content);
    }

    @Test
    @DisplayName("hard_stop 은 recommendedAction 이 answer 와 같은 값이라 붙이지 않는다")
    void hardStopKeepsAnswerOnly() {
        String message = "감염 위험이 의심되는 증상입니다. 즉시 병원에 방문해 주세요.";
        String content = FastApiRagChatAnswerService.composeContent(json("""
                {
                  "answer": "%s",
                  "route": "hard_stop",
                  "recommendedAction": "%s",
                  "systemActions": ["stop_chatbot", "activate_global_medical_summary_report"]
                }
                """.formatted(message, message)));

        assertEquals(message, content);
    }

    @Test
    @DisplayName("insufficient_evidence 는 answer 본문이 이미 같은 안내를 담아 붙이지 않는다")
    void insufficientEvidenceKeepsAnswerOnly() {
        String content = FastApiRagChatAnswerService.composeContent(json("""
                {
                  "answer": "충분히 관련된 근거를 찾지 못했습니다. 시술 병원에 사진과 함께 확인해 주세요.",
                  "route": "insufficient_evidence",
                  "recommendedAction": "시술 병원에 사진과 증상 정보를 함께 전달해 확인해 주세요."
                }
                """));

        assertFalse(content.contains("증상 정보를 함께 전달해"), content);
        assertTrue(content.startsWith("충분히 관련된 근거를"), content);
    }

    @Test
    @DisplayName("answer 가 없으면 실패한다")
    void missingAnswerFails() {
        assertThrows(IllegalStateException.class, () ->
                FastApiRagChatAnswerService.composeContent(json("""
                        {"route": "rag_answer", "recommendedAction": "화상 상담을 권장드립니다."}
                        """)));

        assertThrows(IllegalStateException.class, () ->
                FastApiRagChatAnswerService.composeContent(json("""
                        {"answer": "   ", "route": "rag_answer"}
                        """)));
    }
}
