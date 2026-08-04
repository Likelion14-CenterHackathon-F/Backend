CREATE TABLE CONSULTATION_SESSION (
    session_id BIGINT NOT NULL AUTO_INCREMENT,
    rtc_channel_name VARCHAR(64) NOT NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    actual_duration_seconds INT NULL,
    appointment_id BIGINT NOT NULL,
    patient_agora_uid INT NULL,
    medical_staff_agora_uid INT NULL,
    patient_language VARCHAR(20) NULL,
    medical_staff_language VARCHAR(20) NULL,
    stt_agent_id VARCHAR(100) NULL,
    session_status VARCHAR(20) NOT NULL,
    stt_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (session_id),
    CONSTRAINT uk_consultation_session_appointment
        UNIQUE (appointment_id),
    CONSTRAINT uk_consultation_session_channel
        UNIQUE (rtc_channel_name)
);

CREATE TABLE TRANSCRIPT_SEGMENT (
    transcript_segment_id BIGINT NOT NULL AUTO_INCREMENT,
    sequence_number INT NOT NULL,
    speaker_role VARCHAR(30) NOT NULL,
    speaker_agora_uid VARCHAR(20) NOT NULL,
    source_language VARCHAR(20) NOT NULL,
    source_text TEXT NOT NULL,
    target_language VARCHAR(20) NULL,
    translated_text TEXT NULL,
    is_final BOOLEAN NOT NULL,
    created_at DATETIME(3) NOT NULL,
    sentence_id BIGINT NOT NULL,
    text_timestamp BIGINT NULL,
    duration_ms INT NULL,
    session_id BIGINT NOT NULL,
    PRIMARY KEY (transcript_segment_id),
    CONSTRAINT fk_transcript_segment_session
        FOREIGN KEY (session_id)
        REFERENCES CONSULTATION_SESSION (session_id),
    CONSTRAINT uk_transcript_session_sentence
        UNIQUE (session_id, sentence_id),
    INDEX idx_transcript_session_sequence (
        session_id,
        sequence_number
    )
);

CREATE TABLE CONSULTATION_SUMMARY (
    summary_id BIGINT NOT NULL AUTO_INCREMENT,
    consulted_at DATETIME NULL,
    hospital_name VARCHAR(255) NULL,
    medical_staff_name VARCHAR(255) NULL,
    translated_summary TEXT NULL,
    consultation_details TEXT NULL,
    session_id BIGINT NOT NULL,
    PRIMARY KEY (summary_id),
    CONSTRAINT fk_consultation_summary_session
        FOREIGN KEY (session_id)
        REFERENCES CONSULTATION_SESSION (session_id)
);

CREATE TABLE SUMMARY_INSTRUCTION (
    instruction_id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NULL,
    sort_order INT NULL,
    patient_completed BOOLEAN NULL,
    completed_at DATETIME NULL,
    summary_id BIGINT NOT NULL,
    PRIMARY KEY (instruction_id),
    CONSTRAINT fk_summary_instruction_summary
        FOREIGN KEY (summary_id)
        REFERENCES CONSULTATION_SUMMARY (summary_id)
);
