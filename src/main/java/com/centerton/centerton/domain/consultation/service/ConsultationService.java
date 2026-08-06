package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.consultation.client.AgoraSttClient;
import com.centerton.centerton.domain.consultation.config.AgoraProperties;
import com.centerton.centerton.domain.consultation.dto.request.JoinConsultationReq;
import com.centerton.centerton.domain.consultation.dto.request.TokenRenewReq;
import com.centerton.centerton.domain.consultation.dto.response.ConsultationEndRes;
import com.centerton.centerton.domain.consultation.dto.response.ConsultationHistoryRes;
import com.centerton.centerton.domain.consultation.dto.response.JoinConsultationRes;
import com.centerton.centerton.domain.consultation.dto.response.TokenRes;
import com.centerton.centerton.domain.consultation.dto.response.TranscriptionRes;
import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultation.entity.enums.SttAgentStatus;
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.consultation.repository.TranscriptSegmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private static final int RECOMMENDED_DURATION_SECONDS = 900;

    private final ConsultationSessionRepository sessionRepository;
    private final TranscriptSegmentRepository transcriptRepository;
    private final ConsultationJoinPolicy joinPolicy;
    private final AgoraRtcTokenService rtcTokenService;
    private final AgoraSttClient sttClient;
    private final AgoraProperties agoraProperties;

    public ConsultationService(
            ConsultationSessionRepository sessionRepository,
            TranscriptSegmentRepository transcriptRepository,
            ConsultationJoinPolicy joinPolicy,
            AgoraRtcTokenService rtcTokenService,
            AgoraSttClient sttClient,
            AgoraProperties agoraProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
        this.joinPolicy = joinPolicy;
        this.rtcTokenService = rtcTokenService;
        this.sttClient = sttClient;
        this.agoraProperties = agoraProperties;
    }

    @Transactional
    public JoinConsultationRes join(Long appointmentId, JoinConsultationReq request) {
        joinPolicy.validateJoin(appointmentId);

        ConsultationSession session = sessionRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> ConsultationSession.create(
                        appointmentId,
                        generateChannelName(appointmentId)
                ));

        if (session.isCompleted()) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_ALREADY_COMPLETED);
        }

        session.registerParticipant(
                request.role(),
                request.agoraUid(),
                request.userLanguage()
        );
        session.start(nowUtc());
        sessionRepository.save(session);

        AgoraRtcTokenService.IssuedRtcToken issuedToken = rtcTokenService.issuePublisherToken(
                session.getRtcChannelName(),
                request.agoraUid()
        );

        return new JoinConsultationRes(
                appointmentId,
                session.getSessionId(),
                agoraProperties.getAppId(),
                session.getRtcChannelName(),
                request.agoraUid(),
                issuedToken.token(),
                issuedToken.expiresAt(),
                request.role(),
                session.getLanguage(request.role()),
                session.getPeerLanguage(request.role()),
                agoraProperties.getStt().getPubBotUid(),
                RECOMMENDED_DURATION_SECONDS,
                null
        );
    }

    public TokenRes renewToken(Long appointmentId, TokenRenewReq request) {
        ConsultationSession session = getSession(appointmentId);

        if (session.isCompleted()) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_ALREADY_COMPLETED);
        }

        Integer agoraUid = session.getAgoraUid(request.role());
        if (agoraUid == null) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_PARTICIPANTS_NOT_READY);
        }

        AgoraRtcTokenService.IssuedRtcToken issuedToken = rtcTokenService.issuePublisherToken(
                session.getRtcChannelName(),
                agoraUid
        );

        return new TokenRes(issuedToken.token(), issuedToken.expiresAt());
    }

    @Transactional
    public TranscriptionRes startTranscription(Long appointmentId) {
        ConsultationSession session = sessionRepository.findByAppointmentIdForUpdate(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));

        if (session.isCompleted()) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_ALREADY_COMPLETED);
        }

        if (session.isSttStartingOrRunning() && session.getSttAgentId() != null) {
            return toTranscriptionRes(session);
        }

        if (!session.isReadyForStt()) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_PARTICIPANTS_NOT_READY);
        }

        session.markSttStarting();
        sessionRepository.saveAndFlush(session);

        try {
            String agentId = sttClient.startAgent(session);
            session.markSttRunning(agentId);
        } catch (RuntimeException exception) {
            session.markSttFailed();
            throw exception;
        }

        return toTranscriptionRes(session);
    }

    public TranscriptionRes getTranscriptionStatus(Long appointmentId) {
        return toTranscriptionRes(getSession(appointmentId));
    }

    @Transactional
    public ConsultationEndRes end(Long appointmentId) {
        ConsultationSession session = sessionRepository.findByAppointmentIdForUpdate(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));

        if (session.isCompleted()) {
            return toEndRes(session);
        }

        session.markCompleting();

        if (session.getSttAgentId() != null
                && session.getSttStatus() != SttAgentStatus.STOPPED) {
            session.markSttStopping();

            try {
                sttClient.stopAgent(session.getSttAgentId());
                session.markSttStopped();
            } catch (RestClientException exception) {
                session.markSttFailed();
                log.warn(
                        "Agora STT Agent 종료 실패. sessionId={}, agentId={}",
                        session.getSessionId(),
                        session.getSttAgentId(),
                        exception
                );
            }
        }

        session.complete(nowUtc());
        return toEndRes(session);
    }

    public List<ConsultationHistoryRes> getHistory() {
        return sessionRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(session -> new ConsultationHistoryRes(
                        session.getAppointmentId(),
                        session.getSessionId(),
                        session.getStartedAt(),
                        session.getEndedAt(),
                        session.getActualDurationSeconds(),
                        transcriptRepository.existsByConsultationSessionSessionId(
                                session.getSessionId()
                        )
                ))
                .toList();
    }

    private ConsultationSession getSession(Long appointmentId) {
        return sessionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));
    }

    private TranscriptionRes toTranscriptionRes(ConsultationSession session) {
        return new TranscriptionRes(
                session.getSessionId(),
                session.getSttAgentId(),
                session.getSttStatus()
        );
    }

    private ConsultationEndRes toEndRes(ConsultationSession session) {
        return new ConsultationEndRes(
                session.getSessionId(),
                session.getSessionStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getActualDurationSeconds()
        );
    }

    private String generateChannelName(Long appointmentId) {
        return "consultation_"
                + appointmentId
                + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
