package com.centerton.centerton.domain.aichat.safety;

import java.util.List;

/**
 * 응급 룰 매칭 결과.
 *
 * <p>{@code frontendMessage}, {@code ruleIds}, {@code systemActions} 는 매칭된 모든 룰을
 * 반영한다. 첫 룰만 전달하면 필러 혈관 경고가 일반 호흡·시야 안내로 대체된다.
 */
public record EmergencyRuleMatch(
        String ruleId,
        String ruleName,
        String matchedSignals,
        String frontendMessage,
        List<String> ruleIds,
        List<String> systemActions
) {
}
