package com.centerton.centerton.domain.consultation.entity;

import com.centerton.centerton.domain.consultation.entity.enums.ConsultationSessionStatus;
import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.centerton.centerton.domain.consultation.entity.enums.SttAgentStatus;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "CONSULTATION_SESSION",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_consultation_session_appointment",
                        columnNames = "appointment_id"
                ),
                @UniqueConstraint(
                        name = "uk_consultation_session_channel",
                        columnNames = "rtc_channel_name"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsultationSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "rtc_channel_name", nullable = false, length = 64)
    private String rtcChannelName;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "patient_agora_uid")
    private Integer patientAgoraUid;

    @Column(name = "medical_staff_agora_uid")
    private Integer medicalStaffAgoraUid;

    @Column(name = "patient_language", length = 20)
    private String patientLanguage;

    @Column(name = "medical_staff_language", length = 20)
    private String medicalStaffLanguage;

    @Column(name = "stt_agent_id", length = 100)
    private String sttAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private ConsultationSessionStatus sessionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "stt_status", nullable = false, length = 20)
    private SttAgentStatus sttStatus;

    public static ConsultationSession create(Long appointmentId, String rtcChannelName) {
        return new ConsultationSession(
                null,
                rtcChannelName,
                null,
                null,
                null,
                appointmentId,
                null,
                null,
                null,
                null,
                null,
                ConsultationSessionStatus.READY,
                SttAgentStatus.NOT_STARTED
        );
    }

    public void registerParticipant(ParticipantRole role, Integer agoraUid, String language) {
        if (role == ParticipantRole.PATIENT) {
            patientAgoraUid = agoraUid;
            patientLanguage = language;
            return;
        }

        medicalStaffAgoraUid = agoraUid;
        medicalStaffLanguage = language;
    }

    public void start(LocalDateTime startedAt) {
        if (sessionStatus == ConsultationSessionStatus.COMPLETED) {
            return;
        }

        if (this.startedAt == null) {
            this.startedAt = startedAt;
        }
        sessionStatus = ConsultationSessionStatus.IN_PROGRESS;
    }

    public void markSttStarting() {
        sttStatus = SttAgentStatus.STARTING;
    }

    public void markSttRunning(String sttAgentId) {
        this.sttAgentId = sttAgentId;
        sttStatus = SttAgentStatus.RUNNING;
    }

    public void markSttStopping() {
        sttStatus = SttAgentStatus.STOPPING;
    }

    public void markSttStopped() {
        sttStatus = SttAgentStatus.STOPPED;
    }

    public void markSttFailed() {
        sttStatus = SttAgentStatus.FAILED;
    }

    public void markCompleting() {
        if (sessionStatus != ConsultationSessionStatus.COMPLETED) {
            sessionStatus = ConsultationSessionStatus.COMPLETING;
        }
    }

    public void complete(LocalDateTime endedAt) {
        if (sessionStatus == ConsultationSessionStatus.COMPLETED) {
            return;
        }

        this.endedAt = endedAt;
        this.actualDurationSeconds = calculateDurationSeconds(endedAt);
        this.sessionStatus = ConsultationSessionStatus.COMPLETED;
    }

    public boolean isCompleted() {
        return sessionStatus == ConsultationSessionStatus.COMPLETED;
    }

    public boolean isSttStartingOrRunning() {
        return sttStatus == SttAgentStatus.STARTING
                || sttStatus == SttAgentStatus.RUNNING;
    }

    public boolean isReadyForStt() {
        return patientAgoraUid != null
                && medicalStaffAgoraUid != null
                && patientLanguage != null
                && medicalStaffLanguage != null;
    }

    public Integer getAgoraUid(ParticipantRole role) {
        return role == ParticipantRole.PATIENT
                ? patientAgoraUid
                : medicalStaffAgoraUid;
    }

    public String getLanguage(ParticipantRole role) {
        return role == ParticipantRole.PATIENT
                ? patientLanguage
                : medicalStaffLanguage;
    }

    public String getPeerLanguage(ParticipantRole role) {
        return role == ParticipantRole.PATIENT
                ? medicalStaffLanguage
                : patientLanguage;
    }

    public ParticipantRole resolveParticipantRole(Integer agoraUid) {
        if (agoraUid != null && agoraUid.equals(patientAgoraUid)) {
            return ParticipantRole.PATIENT;
        }

        if (agoraUid != null && agoraUid.equals(medicalStaffAgoraUid)) {
            return ParticipantRole.MEDICAL_STAFF;
        }

        throw new IllegalArgumentException("상담 세션에 등록되지 않은 Agora UID입니다.");
    }

    private int calculateDurationSeconds(LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            return 0;
        }

        long seconds = Math.max(0, Duration.between(startedAt, endedAt).getSeconds());
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }
}
