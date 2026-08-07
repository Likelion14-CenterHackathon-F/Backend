package com.centerton.centerton.domain.consultation.client;

import com.centerton.centerton.domain.consultation.config.AgoraProperties;
import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.domain.consultation.service.AgoraRtcTokenService;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class AgoraSttClient {

    private final RestClient agoraSttRestClient;
    private final AgoraProperties agoraProperties;
    private final AgoraRtcTokenService rtcTokenService;

    public AgoraSttClient(
            @Qualifier("agoraSttRestClient")
            RestClient agoraSttRestClient,
            AgoraProperties agoraProperties,
            AgoraRtcTokenService rtcTokenService
    ) {
        this.agoraSttRestClient = agoraSttRestClient;
        this.agoraProperties = agoraProperties;
        this.rtcTokenService = rtcTokenService;
    }

    public String startAgent(ConsultationSession session) {
        if (!session.isReadyForStt()) {
            throw new BaseException(
                    ConsultationErrorCode.CONSULTATION_PARTICIPANTS_NOT_READY
            );
        }

        try {
            int subBotUid = agoraProperties.getStt().getSubBotUid();
            int pubBotUid = agoraProperties.getStt().getPubBotUid();

            String subBotToken = rtcTokenService
                    .issuePublisherToken(
                            session.getRtcChannelName(),
                            subBotUid
                    )
                    .token();

            String pubBotToken = rtcTokenService
                    .issuePublisherToken(
                            session.getRtcChannelName(),
                            pubBotUid
                    )
                    .token();

            Map<String, Object> requestBody = createStartRequest(
                    session,
                    subBotUid,
                    subBotToken,
                    pubBotUid,
                    pubBotToken
            );

            JsonNode response = agoraSttRestClient
                    .post()
                    .uri(
                            "/projects/{appId}/join",
                            agoraProperties.getAppId()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String agentId = extractAgentId(response);

            if (agentId == null || agentId.isBlank()) {
                throw new BaseException(
                        ConsultationErrorCode.STT_AGENT_START_FAILED
                );
            }

            return agentId;

        } catch (BaseException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BaseException(
                    ConsultationErrorCode.STT_AGENT_START_FAILED
            );
        }
    }

    public void stopAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return;
        }

        try {
            agoraSttRestClient
                    .post()
                    .uri(
                            "/projects/{appId}/agents/{agentId}/leave",
                            agoraProperties.getAppId(),
                            agentId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            /*
             * 기존에는 시작 실패 에러코드를 사용했으나,
             * 종료 실패 전용 에러코드로 분리합니다.
             */
            throw new BaseException(
                    ConsultationErrorCode.STT_AGENT_STOP_FAILED
            );
        }
    }

    private Map<String, Object> createStartRequest(
            ConsultationSession session,
            int subBotUid,
            String subBotToken,
            int pubBotUid,
            String pubBotToken
    ) {
        List<String> languages = Stream.of(
                        session.getPatientLanguage(),
                        session.getMedicalStaffLanguage()
                )
                .filter(Objects::nonNull)
                .filter(language -> !language.isBlank())
                .distinct()
                .toList();

        List<Map<String, Object>> uidLanguagesConfig = List.of(
                Map.of(
                        "uid",
                        String.valueOf(session.getPatientAgoraUid()),
                        "languages",
                        List.of(session.getPatientLanguage())
                ),
                Map.of(
                        "uid",
                        String.valueOf(session.getMedicalStaffAgoraUid()),
                        "languages",
                        List.of(session.getMedicalStaffLanguage())
                )
        );

        Map<String, Object> rtcConfig = new LinkedHashMap<>();
        rtcConfig.put(
                "channelName",
                session.getRtcChannelName()
        );
        rtcConfig.put(
                "subBotUid",
                String.valueOf(subBotUid)
        );
        rtcConfig.put(
                "subBotToken",
                subBotToken
        );
        rtcConfig.put(
                "pubBotUid",
                String.valueOf(pubBotUid)
        );
        rtcConfig.put(
                "pubBotToken",
                pubBotToken
        );
        rtcConfig.put(
                "subscribeAudioUids",
                List.of(
                        String.valueOf(
                                session.getPatientAgoraUid()
                        ),
                        String.valueOf(
                                session.getMedicalStaffAgoraUid()
                        )
                )
        );

        Map<String, Object> requestBody = new LinkedHashMap<>();

        requestBody.put(
                "name",
                "consultation-"
                        + session.getSessionId()
                        + "-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
        );
        requestBody.put("languages", languages);
        requestBody.put(
                "uidLanguagesConfig",
                uidLanguagesConfig
        );
        requestBody.put(
                "maxIdleTime",
                agoraProperties.getStt()
                        .getMaxIdleTimeSeconds()
        );
        requestBody.put("rtcConfig", rtcConfig);

        List<Map<String, Object>> translationPairs =
                createTranslationPairs(session);

        if (!translationPairs.isEmpty()) {
            requestBody.put(
                    "translateConfig",
                    Map.of(
                            "languages",
                            translationPairs
                    )
            );
        }

        List<String> keywords =
                agoraProperties.getStt().getKeywords();

        if (keywords != null && !keywords.isEmpty()) {
            requestBody.put("keywords", keywords);
        }

        return requestBody;
    }

    private List<Map<String, Object>> createTranslationPairs(
            ConsultationSession session
    ) {
        String patientLanguage =
                session.getPatientLanguage();

        String medicalLanguage =
                session.getMedicalStaffLanguage();

        if (patientLanguage == null
                || medicalLanguage == null
                || patientLanguage.isBlank()
                || medicalLanguage.isBlank()
                || patientLanguage.equals(medicalLanguage)) {
            return List.of();
        }

        List<Map<String, Object>> pairs =
                new ArrayList<>();

        pairs.add(Map.of(
                "source",
                patientLanguage,
                "target",
                List.of(medicalLanguage)
        ));

        pairs.add(Map.of(
                "source",
                medicalLanguage,
                "target",
                List.of(patientLanguage)
        ));

        return pairs;
    }

    private String extractAgentId(JsonNode response) {
        if (response == null || response.isNull()) {
            return null;
        }

        String directAgentId = firstText(
                response,
                "agent_id",
                "agentId",
                "task_id",
                "taskId"
        );

        if (directAgentId != null) {
            return directAgentId;
        }

        JsonNode data = response.path("data");

        return firstText(
                data,
                "agent_id",
                "agentId",
                "task_id",
                "taskId"
        );
    }

    private String firstText(
            JsonNode node,
            String... fieldNames
    ) {
        if (node == null || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);

            if (value != null
                    && !value.isNull()
                    && !value.asText().isBlank()) {
                return value.asText();
            }
        }

        return null;
    }
}