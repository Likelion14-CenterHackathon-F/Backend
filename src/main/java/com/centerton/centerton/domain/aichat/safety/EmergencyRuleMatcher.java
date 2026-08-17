package com.centerton.centerton.domain.aichat.safety;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 응급 hard-stop 룰 매칭. {@code rag_rulebook/rules/emergency_rules.json} 의 계약을 구현한다.
 *
 * <p>Python 참조 구현({@code rag_rulebook/tools/emergency_matcher.py})과 동일하게 동작해야 한다.
 * 갈라지면 Spring 이 차단한 것과 FastAPI 가 차단한 것이 달라지므로, 회귀 66건을 양쪽에서
 * 채점한다. {@code AiChatEmergencyRuleServiceTest} 를 함께 유지해야 한다.
 *
 * <p>계약 세 가지.
 *
 * <ol>
 *   <li>{@code trigger_keywords} 는 공백을 제거한 compact 형태에서, {@code trigger_patterns} 는
 *       공백을 보존한 spaced 형태에서 평가한다. 공백을 제거하면 "약을 먹고 열이 내렸어요" 가
 *       "고열이" 를 포함하게 되어 안심 문의가 차단된다.
 *   <li>{@code negation_guards} 는 매칭된 span 직후에 앵커링해 평가한다. 앵커링하지 않으면
 *       "고름이 나오고 통증은 없어요" 에서 통증의 부정이 고름 트리거를 취소한다.
 *   <li>매칭된 모든 룰을 반환한다. 첫 룰만 반환하면 필러 혈관 경고(RISK-07)가 일반
 *       호흡·시야 안내(RISK-05)로 대체된다.
 * </ol>
 *
 * <p>부정어 "안" 은 억제 토큰에 넣지 않는다. "열이 안 떨어져요", "피가 안 멈춰요" 는 증상이
 * 지속된다는 뜻이므로 실제 응급이다.
 */
public final class EmergencyRuleMatcher {

    private static final String POLICY_KEYWORD_OR_PATTERN = "any_keyword_or_pattern";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final List<EmergencyRule> rules;
    private final List<Pattern> negationGuards;
    private final Map<String, String> numberAliases;

    public EmergencyRuleMatcher(
            List<EmergencyRule> rules,
            List<Pattern> negationGuards,
            Map<String, String> numberAliases
    ) {
        this.rules = List.copyOf(rules);
        this.negationGuards = List.copyOf(negationGuards);
        this.numberAliases = Map.copyOf(numberAliases);
    }


    private String applyAliases(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : numberAliases.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** {@code trigger_keywords} 평가용. 공백을 모두 제거한다. */
    String normalizeCompact(String value) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase();
        text = WHITESPACE.matcher(text).replaceAll("");
        return applyAliases(text);
    }

    /** {@code trigger_patterns} 평가용. 공백을 단일 공백으로 축약해 어절 경계를 보존한다. */
    String normalizeSpaced(String value) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase();
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        return applyAliases(text);
    }


    /** 트리거 직후 텍스트가 증상 부재를 보고하는지 판단한다. guard 는 모두 {@code ^} 앵커다. */
    boolean isNegated(String remainder) {
        for (Pattern guard : negationGuards) {
            if (guard.matcher(remainder).find()) {
                return true;
            }
        }
        return false;
    }


    private RuleHit keywordHit(EmergencyRule rule, String compact) {
        for (String keyword : rule.keywords()) {
            String needle = normalizeCompact(keyword);
            if (needle.isEmpty()) {
                continue;
            }
            int start = compact.indexOf(needle);
            while (start >= 0) {
                int end = start + needle.length();
                // 첫 출현만 보면 "고름은 없지만 다른 곳에서 고름이 나와요" 가 미탐된다.
                if (!isNegated(compact.substring(end))) {
                    return new RuleHit(rule.id(), rule.name(), "keyword", keyword, needle,
                            rule.frontendMessage(), rule.systemActions());
                }
                start = compact.indexOf(needle, start + 1);
            }
        }
        return null;
    }

    private RuleHit patternHit(EmergencyRule rule, String spaced) {
        for (Pattern pattern : rule.patterns()) {
            Matcher matcher = pattern.matcher(spaced);
            while (matcher.find()) {
                if (!isNegated(spaced.substring(matcher.end()))) {
                    return new RuleHit(rule.id(), rule.name(), "pattern", pattern.pattern(),
                            matcher.group(), rule.frontendMessage(), rule.systemActions());
                }
            }
        }
        return null;
    }

    /** 매칭된 모든 룰을 룰북 선언 순서대로 반환한다. */
    public List<RuleHit> match(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        String compact = normalizeCompact(question);
        String spaced = normalizeSpaced(question);
        List<RuleHit> hits = new ArrayList<>();

        for (EmergencyRule rule : rules) {
            RuleHit hit = keywordHit(rule, compact);
            if (hit == null && POLICY_KEYWORD_OR_PATTERN.equals(rule.matchPolicy())) {
                hit = patternHit(rule, spaced);
            }
            if (hit != null) {
                hits.add(hit);
            }
        }
        return List.copyOf(hits);
    }

    public boolean isHardStop(String question) {
        return !match(question).isEmpty();
    }

    /** 매칭된 모든 룰의 안내 문구를 중복 없이 이어붙인다. */
    public static String buildMessage(List<RuleHit> hits) {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (RuleHit hit : hits) {
            String message = hit.frontendMessage() == null ? "" : hit.frontendMessage().strip();
            if (!message.isEmpty()) {
                seen.putIfAbsent(message, Boolean.TRUE);
            }
        }
        return String.join("\n", seen.keySet());
    }

    /** 매칭된 모든 룰의 system_actions 를 중복 없이 모은다. */
    public static List<String> collectSystemActions(List<RuleHit> hits) {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (RuleHit hit : hits) {
            for (String action : hit.systemActions()) {
                seen.putIfAbsent(action, Boolean.TRUE);
            }
        }
        return List.copyOf(seen.keySet());
    }


    public record EmergencyRule(
            String id,
            String name,
            List<String> keywords,
            List<Pattern> patterns,
            String matchPolicy,
            String frontendMessage,
            List<String> systemActions
    ) {
    }

    public record RuleHit(
            String ruleId,
            String ruleName,
            String matchedBy,
            String evidence,
            String matchedText,
            String frontendMessage,
            List<String> systemActions
    ) {
    }
}
