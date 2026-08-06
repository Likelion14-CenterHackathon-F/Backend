package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateRes;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkVerifyReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkVerifyRes;

public interface PatientAccessLinkService {

    PatientAccessLinkCreateRes createAccessLink(Long patientId, PatientAccessLinkCreateReq request);

    PatientAccessLinkVerifyRes verifyAccessLink(PatientAccessLinkVerifyReq request);
}
