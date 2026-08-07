package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.exception.PatientNotFoundException;
import com.centerton.centerton.domain.patient.exception.PatientSettingsInvalidException;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.domain.patient.web.dto.PatientSettingsUpdateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private final PatientRepository patientRepository;

    @Override
    @Transactional
    public void updateSettings(Long patientId, PatientSettingsUpdateReq request) {
        validateSettingsRequest(request);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(PatientNotFoundException::new);

        String nationality = normalizeCountryCode(request.nationality());
        String timezoneId = request.timezoneId().trim();
        patient.updateSettings(request.language(), nationality, timezoneId);
    }

    private void validateSettingsRequest(PatientSettingsUpdateReq request) {
        if (request == null
                || request.language() == null
                || !StringUtils.hasText(request.nationality())
                || !StringUtils.hasText(request.timezoneId())) {
            throw new PatientSettingsInvalidException();
        }

        validateCountryCode(request.nationality());
        validateTimezoneId(request.timezoneId());
    }

    private void validateCountryCode(String nationality) {
        if (!ISO_COUNTRY_CODES.contains(normalizeCountryCode(nationality))) {
            throw new PatientSettingsInvalidException();
        }
    }

    private void validateTimezoneId(String timezoneId) {
        try {
            ZoneId.of(timezoneId.trim());
        } catch (DateTimeException e) {
            throw new PatientSettingsInvalidException();
        }
    }

    private String normalizeCountryCode(String nationality) {
        return nationality.trim().toUpperCase(Locale.ROOT);
    }
}
