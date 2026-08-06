package com.centerton.centerton.domain.patient.web.dto;

import java.time.LocalDate;

public record PatientAccessLinkVerifyReq(
        String token,
        LocalDate birthDate
) {
}
