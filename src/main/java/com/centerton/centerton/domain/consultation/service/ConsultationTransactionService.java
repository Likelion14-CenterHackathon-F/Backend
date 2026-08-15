package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.consultation.dto.request.JoinConsultationReq;
import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultation.entity.enums.SttAgentStatus;
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ConsultationService에서 외부 Agora API 호출과 DB 트랜잭션을
 * 분리하기 위한 트랜잭션 전용 서비스입니다.
 * <p>
 * 각 메서드는 짧은 DB 작업만 수행하고 즉시 커밋합니다.
 */
@Service
@Transactional(readOnly = true)
public class ConsultationTransactionService {

    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationJoinPolicy joinPolicy;
    private final AppointmentRepository appointmentRepository;

    public ConsultationTransactionService(
            ConsultationSessionRepository sessionRepository,
            ConsultationJoinPolicy joinPolicy,
            AppointmentRepository appointmentRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.joinPolicy = joinPolicy;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsultationSession joinOrCreate(
            Long patientId,
            Long appointmentId,
            JoinConsultationReq request,
            String rtcChannelName,
            LocalDateTime joinedAt
    ) {
        joinPolicy.validateJoin(patientId, appointmentId);

        ConsultationSession session = sessionRepository
                .findByAppointmentIdForUpdate(appointmentId)
                .orElseGet(() -> ConsultationSession.create(
                        appointmentId,
                        rtcChannelName
                ));

        registerParticipant(session, request, joinedAt);

        /*
         * appointment_id unique 충돌을 현재 트랜잭션 안에서
         * 확실히 감지하기 위해 saveAndFlush()를 사용합니다.
         */
        return sessionRepository.saveAndFlush(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ConsultationSession> joinExisting(
            Long patientId,
            Long appointmentId,
            JoinConsultationReq request,
            LocalDateTime joinedAt
    ) {
        joinPolicy.validateJoin(patientId, appointmentId);

        return sessionRepository.findByAppointmentIdForUpdate(appointmentId)
                .map(session -> {
                    registerParticipant(session, request, joinedAt);
                    return sessionRepository.saveAndFlush(session);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SttStartPreparation prepareSttStart(Long appointmentId) {
        ConsultationSession session = getSessionForUpdate(appointmentId);

        validateNotCompleted(session);

        if (session.isSttStartingOrRunning()) {
            return new SttStartPreparation(false, session);
        }

        if (!session.isReadyForStt()) {
            throw new BaseException(
                    ConsultationErrorCode.CONSULTATION_PARTICIPANTS_NOT_READY
            );
        }

        session.markSttStarting();
        ConsultationSession savedSession =
                sessionRepository.saveAndFlush(session);

        return new SttStartPreparation(true, savedSession);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsultationSession markSttRunning(
            Long appointmentId,
            String sttAgentId
    ) {
        ConsultationSession session = getSessionForUpdate(appointmentId);

        validateNotCompleted(session);

        session.markSttRunning(sttAgentId);
        return sessionRepository.saveAndFlush(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsultationSession markSttFailed(Long appointmentId) {
        ConsultationSession session = getSessionForUpdate(appointmentId);

        if (!session.isCompleted()) {
            session.markSttFailed();
        }

        return sessionRepository.saveAndFlush(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsultationEndPreparation prepareEnd(Long appointmentId) {
        ConsultationSession session = getSessionForUpdate(appointmentId);

        if (session.isCompleted()) {
            return new ConsultationEndPreparation(
                    session,
                    false,
                    null
            );
        }

        session.markCompleting();

        String sttAgentId = session.getSttAgentId();
        boolean stopRequired = sttAgentId != null
                && !sttAgentId.isBlank()
                && session.getSttStatus() != SttAgentStatus.STOPPED;

        if (stopRequired) {
            session.markSttStopping();
        }

        ConsultationSession savedSession =
                sessionRepository.saveAndFlush(session);

        return new ConsultationEndPreparation(
                savedSession,
                stopRequired,
                sttAgentId
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsultationSession completeEnd(
            Long appointmentId,
            boolean stopRequired,
            boolean sttStopSucceeded,
            LocalDateTime endedAt
    ) {
        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));
        ConsultationSession session = getSessionForUpdate(appointmentId);

        if (session.isCompleted()) {
            appointment.complete();
            return session;
        }

        if (stopRequired) {
            if (sttStopSucceeded) {
                session.markSttStopped();
            } else {
                session.markSttFailed();
            }
        }

        session.complete(endedAt);
        appointment.complete();
        return sessionRepository.saveAndFlush(session);
    }

    private void registerParticipant(
            ConsultationSession session,
            JoinConsultationReq request,
            LocalDateTime joinedAt
    ) {
        validateNotCompleted(session);

        session.registerParticipant(
                request.role(),
                request.agoraUid(),
                request.userLanguage().getAgoraCode()
        );

        session.start(joinedAt);
    }

    private ConsultationSession getSessionForUpdate(Long appointmentId) {
        return sessionRepository.findByAppointmentIdForUpdate(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));
    }

    private void validateNotCompleted(ConsultationSession session) {
        if (session.isCompleted()) {
            throw new BaseException(
                    ConsultationErrorCode.CONSULTATION_ALREADY_COMPLETED
            );
        }
    }

    public record SttStartPreparation(
            boolean startRequired,
            ConsultationSession session
    ) {
    }

    public record ConsultationEndPreparation(
            ConsultationSession session,
            boolean stopRequired,
            String sttAgentId
    ) {
    }
}
