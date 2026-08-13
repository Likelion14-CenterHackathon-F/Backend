package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.web.dto.PatientSettingsUpdateReq;
import com.centerton.centerton.domain.patient.web.dto.PatientSettingsUpdateRes;

public interface PatientService {

    PatientSettingsUpdateRes updateSettings(
            Long patientId,
            PatientSettingsUpdateReq request
    );
}
