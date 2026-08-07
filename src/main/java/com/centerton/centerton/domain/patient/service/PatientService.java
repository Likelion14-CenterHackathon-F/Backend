package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.web.dto.PatientSettingsUpdateReq;

public interface PatientService {

    void updateSettings(Long patientId, PatientSettingsUpdateReq request);
}
