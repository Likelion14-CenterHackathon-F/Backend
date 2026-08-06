package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateRes;

public interface PatientAccessLinkService {

    PatientAccessLinkCreateRes createAccessLink(Long patientId, PatientAccessLinkCreateReq req);
}
