package com.centerton.centerton.domain.aichat.safety;

import com.centerton.centerton.domain.aichat.config.AiChatEmergencyRuleProperties;
import com.centerton.centerton.domain.aichat.safety.EmergencyRuleMatcher.EmergencyRule;
import com.centerton.centerton.domain.aichat.safety.EmergencyRuleMatcher.RuleHit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 응급 룰 파일을 읽어 {@link EmergencyRuleMatcher} 에 넘긴다.
 *
 * <p>로딩만 담당한다. 정규화·매칭·부정 억제는 전부 매처에 있으며, 매칭 로직을 여기에 다시
 * 쓰면 FastAPI 구현과 갈라진다.
 */
@Slf4j
@Service
public class AiChatEmergencyRuleService {

    private final EmergencyRuleMatcher matcher;

    public AiChatEmergencyRuleService(
            AiChatEmergencyRuleProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        RulesFile rulesFile = parseRulesFile(
                readRoot(properties.getPath(), resourceLoader, objectMapper));
        this.matcher = buildMatcher(rulesFile);

        log.info(
                "AI 채팅 응급 룰 로드 완료. path={}, version={}, ruleCount={}, guardCount={}",
                properties.getPath(),
                rulesFile.version(),
                rulesFile.rules().size(),
                rulesFile.negationGuards().size()
        );

        if (rulesFile.negationGuards().isEmpty()) {
            log.warn(
                    "negation_guards 가 비어 있습니다. '고름은 없어요' 같은 증상 부재 보고가"
                            + " 응급으로 차단됩니다. 룰북 파일을 확인하세요."
            );
        }
    }

    /**
     * {@code ruleId} 는 첫 룰이지만 {@code frontendMessage}, {@code ruleIds},
     * {@code systemActions} 는 매칭된 전체를 반영한다.
     */
    public Optional<EmergencyRuleMatch> findMatch(String question) {
        List<RuleHit> hits = matcher.match(question);
        if (hits.isEmpty()) {
            return Optional.empty();
        }

        List<String> ruleIds = new ArrayList<>();
        List<String> signals = new ArrayList<>();
        for (RuleHit hit : hits) {
            ruleIds.add(hit.ruleId());
            signals.add(hit.ruleId() + ":" + hit.matchedText());
        }

        RuleHit first = hits.get(0);
        return Optional.of(new EmergencyRuleMatch(
                first.ruleId(),
                first.ruleName(),
                String.join(", ", signals),
                EmergencyRuleMatcher.buildMessage(hits),
                List.copyOf(ruleIds),
                EmergencyRuleMatcher.collectSystemActions(hits)
        ));
    }


    /** 테스트가 프로덕션과 같은 파싱 경로를 쓰도록 public 이다. */
    public static EmergencyRuleMatcher buildMatcher(JsonNode root) {
        return buildMatcher(parseRulesFile(root));
    }

    private static EmergencyRuleMatcher buildMatcher(RulesFile rulesFile) {
        return new EmergencyRuleMatcher(
                rulesFile.rules(),
                rulesFile.negationGuards(),
                rulesFile.numberAliases()
        );
    }

    private static JsonNode readRoot(
            String path,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("응급 룰 파일 경로가 비어 있습니다.");
        }
        try (InputStream inputStream = openInputStream(path, resourceLoader)) {
            return objectMapper.readTree(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("응급 룰 파일을 읽지 못했습니다: " + path, exception);
        }
    }

    private static RulesFile parseRulesFile(JsonNode root) {
        JsonNode rulesNode = root.path("rules");
        if (!rulesNode.isArray() || rulesNode.isEmpty()) {
            throw new IllegalStateException("응급 룰 파일에 rules 배열이 없습니다.");
        }

        JsonNode normalization = root.path("normalization");
        String patternForm = normalization.path("pattern_text_form").asText("");
        if (!"spaced".equals(patternForm)) {
            // 계약이 바뀌었거나 오래된 룰북이다. 공백 제거 텍스트에 패턴을 적용하면
            // '먹고 열이' 가 '고열이' 로 오탐된다.
            throw new IllegalStateException(
                    "룰북의 normalization.pattern_text_form 이 'spaced' 가 아닙니다: " + patternForm
            );
        }

        return new RulesFile(
                root.path("version").asText("unknown"),
                parseRules(rulesNode),
                parseGuards(root.path("negation_guards").path("patterns")),
                parseAliases(normalization.path("number_aliases"))
        );
    }

    private static InputStream openInputStream(String path, ResourceLoader resourceLoader)
            throws IOException {
        if (path.startsWith("classpath:")
                || path.startsWith("file:")
                || path.startsWith("http:")
                || path.startsWith("https:")) {
            Resource resource = resourceLoader.getResource(path);
            return resource.getInputStream();
        }
        return Files.newInputStream(Path.of(path));
    }

    private static List<EmergencyRule> parseRules(JsonNode rulesNode) {
        List<EmergencyRule> parsed = new ArrayList<>();
        for (JsonNode ruleNode : rulesNode) {
            String id = ruleNode.path("id").asText("unknown");
            parsed.add(new EmergencyRule(
                    id,
                    ruleNode.path("name").asText("unknown"),
                    parseStrings(ruleNode.path("trigger_keywords")),
                    parsePatterns(id, ruleNode.path("trigger_patterns")),
                    ruleNode.path("match_policy").asText("any_keyword"),
                    ruleNode.path("frontend_message").asText(""),
                    parseStrings(ruleNode.path("system_actions"))
            ));
        }
        return List.copyOf(parsed);
    }

    private static List<Pattern> parseGuards(JsonNode arrayNode) {
        List<Pattern> guards = new ArrayList<>();
        for (String expression : parseStrings(arrayNode)) {
            if (!expression.startsWith("^")) {
                // 앵커링되지 않은 guard 는 문장 뒤쪽의 부정으로 실제 응급을 취소한다.
                throw new IllegalStateException(
                        "negation_guard 가 '^' 로 앵커링되지 않았습니다: " + expression
                );
            }
            if (expression.contains("안")) {
                // '안' 은 증상 지속을 뜻한다. 억제 토큰에 들어가면 실제 응급이 미탐된다.
                throw new IllegalStateException(
                        "negation_guard 에 '안' 이 포함되어 있습니다: " + expression
                );
            }
            guards.add(compilePattern("negation_guard", expression));
        }
        return List.copyOf(guards);
    }

    private static Map<String, String> parseAliases(JsonNode objectNode) {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (objectNode.isObject()) {
            objectNode.properties().forEach(entry ->
                    aliases.put(entry.getKey(), entry.getValue().asText()));
        }
        return Map.copyOf(aliases);
    }

    private static List<String> parseStrings(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText();
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static List<Pattern> parsePatterns(String ruleId, JsonNode arrayNode) {
        List<Pattern> patterns = new ArrayList<>();
        for (String expression : parseStrings(arrayNode)) {
            patterns.add(compilePattern(ruleId, expression));
        }
        return List.copyOf(patterns);
    }

    private static Pattern compilePattern(String owner, String expression) {
        try {
            return Pattern.compile(expression);
        } catch (PatternSyntaxException exception) {
            throw new IllegalStateException(
                    "응급 룰 정규식이 올바르지 않습니다. owner=" + owner + ", pattern=" + expression,
                    exception
            );
        }
    }

    private record RulesFile(
            String version,
            List<EmergencyRule> rules,
            List<Pattern> negationGuards,
            Map<String, String> numberAliases
    ) {
    }
}
