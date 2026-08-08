package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.appointment.policy.AppointmentAccessPolicy;
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
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.consultation.repository.TranscriptSegmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ConsultationService {

    private static final int RECOMMENDED_DURATION_SECONDS = 900;

    private final ConsultationSessionRepository sessionRepository;
    private final TranscriptSegmentRepository transcriptRepository;
    private final AppointmentAccessPolicy appointmentAccessPolicy;
    private final ConsultationJoinPolicy joinPolicy;
    private final ConsultationTransactionService transactionService;
    private final AgoraRtcTokenService rtcTokenService;
    private final AgoraSttClient sttClient;
    private final AgoraProperties agoraProperties;

    public ConsultationService(
            ConsultationSessionRepository sessionRepository,
            TranscriptSegmentRepository transcriptRepository,
            AppointmentAccessPolicy appointmentAccessPolicy,
            ConsultationJoinPolicy joinPolicy,
            ConsultationTransactionService transactionService,
            AgoraRtcTokenService rtcTokenService,
            AgoraSttClient sttClient,
            AgoraProperties agoraProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
        this.appointmentAccessPolicy = appointmentAccessPolicy;
        this.joinPolicy = joinPolicy;
        this.transactionService = transactionService;
        this.rtcTokenService = rtcTokenService;
        this.sttClient = sttClient;
        this.agoraProperties = agoraProperties;
    }

    /**
     * 상담 세션 참여
     * <p>
     * 이 메서드 자체에는 트랜잭션을 적용하지 않습니다.
     * 세션 생성 및 참여자 등록은 ConsultationTransactionService의
     * 짧은 트랜잭션에서 처리합니다.
     */
    public JoinConsultationRes join(
            Long patientId,
            Long appointmentId,
            JoinConsultationReq request
    ) {
        joinPolicy.validateJoin(patientId, appointmentId);

        LocalDateTime joinedAt = nowUtc();

        ConsultationSession session;

        try {
            session = transactionService.joinOrCreate(
                    appointmentId,
                    request,
                    generateChannelName(appointmentId),
                    joinedAt
            );
        } catch (DataIntegrityViolationException exception) {
            /*
             * 환자와 의료진이 동시에 처음 join한 경우:
             *
             * 1. 두 요청 모두 세션이 없다고 판단할 수 있음
             * 2. 한 요청이 appointment_id unique insert에 성공
             * 3. 다른 요청은 unique 충돌 발생
             * 4. 이미 생성된 세션을 재조회해 참여자 등록
             */
            session = transactionService.joinExisting(
                            appointmentId,
                            request,
                            joinedAt
                    )
                    .orElseThrow(() -> exception);
        }

        AgoraRtcTokenService.IssuedRtcToken issuedToken =
                rtcTokenService.issuePublisherToken(
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

    @Transactional(readOnly = true)
    public TokenRes renewToken(
            Long patientId,
            Long appointmentId,
            TokenRenewReq request
    ) {
        appointmentAccessPolicy.validateAccess(patientId, appointmentId);
        ConsultationSession session = getSession(appointmentId);

        if (session.isCompleted()) {
            throw new BaseException(
                    ConsultationErrorCode.CONSULTATION_ALREADY_COMPLETED
            );
        }

        Integer agoraUid = session.getAgoraUid(request.role());

        if (agoraUid == null) {
            throw new BaseException(
                    ConsultationErrorCode.CONSULTATION_PARTICIPANTS_NOT_READY
            );
        }

        AgoraRtcTokenService.IssuedRtcToken issuedToken =
                rtcTokenService.issuePublisherToken(
                        session.getRtcChannelName(),
                        agoraUid
                );

        return new TokenRes(
                issuedToken.token(),
                issuedToken.expiresAt()
        );
    }

    /**
     * STT Agent 시작
     * <p>
     * 1. 짧은 DB 트랜잭션에서 STARTING 저장 후 커밋
     * 2. 트랜잭션 밖에서 Agora API 호출
     * 3. 성공 또는 실패 상태를 새로운 트랜잭션으로 저장
     */
    public TranscriptionRes startTranscription(
            Long patientId,
            Long appointmentId
    ) {
        appointmentAccessPolicy.validateAccess(patientId, appointmentId);

        ConsultationTransactionService.SttStartPreparation preparation =
                transactionService.prepareSttStart(appointmentId);

        /*
         * 이미 다른 요청에서 STT를 시작 중이거나 실행 중이라면
         * 새로운 Agora Agent를 생성하지 않고 현재 상태를 반환합니다.
         */
        if (!preparation.startRequired()) {
            return toTranscriptionRes(preparation.session());
        }

        try {
            String sttAgentId =
                    sttClient.startAgent(preparation.session());

            ConsultationSession runningSession =
                    transactionService.markSttRunning(
                            appointmentId,
                            sttAgentId
                    );

            return toTranscriptionRes(runningSession);

        } catch (RuntimeException exception) {
            /*
             * 실패 상태 저장 트랜잭션과 원래 Agora 예외를 분리합니다.
             * FAILED 저장에 실패하더라도 최초 예외가 사라지지 않게
             * suppressed exception으로 추가합니다.
             */
            try {
                transactionService.markSttFailed(appointmentId);
            } catch (RuntimeException statusSaveException) {
                exception.addSuppressed(statusSaveException);
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public TranscriptionRes getTranscriptionStatus(
            Long patientId,
            Long appointmentId
    ) {
        appointmentAccessPolicy.validateAccess(patientId, appointmentId);
        return toTranscriptionRes(getSession(appointmentId));
    }

    /**
     * 상담 종료
     * <p>
     * STT Agent 종료 실패가 상담 자체의 종료를 막지 않도록 처리합니다.
     */
    public ConsultationEndRes end(Long patientId, Long appointmentId) {
        appointmentAccessPolicy.validateAccess(patientId, appointmentId);

        ConsultationTransactionService.ConsultationEndPreparation preparation =
                transactionService.prepareEnd(appointmentId);

        if (preparation.session().isCompleted()) {
            return toEndRes(preparation.session());
        }

        boolean sttStopSucceeded = false;

        if (preparation.stopRequired()) {
            try {
                /*
                 * DB 트랜잭션 및 비관적 락이 없는 상태에서
                 * Agora 외부 API를 호출합니다.
                 */
                sttClient.stopAgent(preparation.sttAgentId());
                sttStopSucceeded = true;

            } catch (BaseException exception) {
                /*
                 * STT Agent 종료에 실패해도 상담 종료는 계속합니다.
                 * 이후 DB에는 STT FAILED + 상담 COMPLETED가 저장됩니다.
                 */
                log.warn(
                        "Agora STT Agent 종료 실패. "
                                + "sessionId={}, agentId={}",
                        preparation.session().getSessionId(),
                        preparation.sttAgentId(),
                        exception
                );
            }
        }

        ConsultationSession completedSession =
                transactionService.completeEnd(
                        appointmentId,
                        preparation.stopRequired(),
                        sttStopSucceeded,
                        nowUtc()
                );

        return toEndRes(completedSession);
    }

    @Transactional(readOnly = true)
    public List<ConsultationHistoryRes> getHistory(Long patientId) {
        return sessionRepository.findAllByPatientIdOrderByStartedAtDesc(patientId)
                .stream()
                .map(session -> new ConsultationHistoryRes(
                        session.getAppointmentId(),
                        session.getSessionId(),
                        session.getStartedAt(),
                        session.getEndedAt(),
                        session.getActualDurationSeconds(),
                        transcriptRepository
                                .existsByConsultationSessionSessionId(
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

    private TranscriptionRes toTranscriptionRes(
            ConsultationSession session
    ) {
        return new TranscriptionRes(
                session.getSessionId(),
                session.getSttAgentId(),
                session.getSttStatus()
        );
    }

    private ConsultationEndRes toEndRes(
            ConsultationSession session
    ) {
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
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
