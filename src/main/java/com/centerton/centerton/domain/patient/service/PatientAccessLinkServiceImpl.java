package com.centerton.centerton.domain.patient.service;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.PatientAccessLink;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkAuthenticationInvalidException;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkExpirationInvalidException;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkExpiredException;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkHashFailedException;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkInvalidException;
import com.centerton.centerton.domain.patient.exception.PatientAccessLinkTokenGenerationFailedException;
import com.centerton.centerton.domain.patient.exception.PatientBirthDateNotMatchedException;
import com.centerton.centerton.domain.patient.exception.PatientNotFoundException;
import com.centerton.centerton.domain.patient.exception.PatientSettingsInvalidException;
import com.centerton.centerton.domain.patient.repository.PatientAccessLinkRepository;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateRes;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkVerifyReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkVerifyRes;
import com.centerton.centerton.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientAccessLinkServiceImpl implements PatientAccessLinkService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

    private final PatientRepository patientRepository;
    private final PatientAccessLinkRepository patientAccessLinkRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock utcClock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${patient.access-link.base-url:https://allway.vercel.app/patient/access}")
    private String patientAccessLinkBaseUrl;

    @Override
    @Transactional
    public PatientAccessLinkCreateRes createAccessLink(Long patientId, PatientAccessLinkCreateReq request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(PatientNotFoundException::new);

        int expiresInMinutes = resolveExpiresInMinutes(request);
        LocalDateTime expiresAt = LocalDateTime.now(utcClock)
                .plusMinutes(expiresInMinutes);
        GeneratedToken generatedToken = generateUniqueToken();

        PatientAccessLink accessLink = patientAccessLinkRepository.save(
                PatientAccessLink.create(generatedToken.tokenHash(), expiresAt, patient)
        );

        String magicLink = buildMagicLink(generatedToken.rawToken());
        return PatientAccessLinkCreateRes.of(accessLink, generatedToken.rawToken(), magicLink);
    }

    @Override
    @Transactional
    public PatientAccessLinkVerifyRes verifyAccessLink(PatientAccessLinkVerifyReq request) {
        validateVerifyRequest(request);

        String tokenHash = hashToken(request.token());
        PatientAccessLink accessLink = patientAccessLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(PatientAccessLinkInvalidException::new);

        if (accessLink.isExpired(LocalDateTime.now(utcClock))) {
            throw new PatientAccessLinkExpiredException();
        }

        Patient patient = accessLink.getPatient();
        if (!patient.getBirthDate().equals(request.birthDate())) {
            throw new PatientBirthDateNotMatchedException();
        }

        applySettings(patient, request);

        String accessToken = jwtTokenProvider.createPatientAccessToken(
                patient.getId(),
                patient.getLanguage()
        );
        return PatientAccessLinkVerifyRes.of(patient.getId(), accessToken);
    }

    private void validateVerifyRequest(PatientAccessLinkVerifyReq request) {
        if (request == null || !StringUtils.hasText(request.token()) || request.birthDate() == null) {
            throw new PatientAccessLinkAuthenticationInvalidException();
        }
    }

    private void applySettings(Patient patient, PatientAccessLinkVerifyReq request) {
        String timezoneId = resolveTimezoneId(request.timezoneId());
        patient.updateSettings(request.language(), null, timezoneId);
    }

    private String resolveTimezoneId(String timezoneId) {
        if (timezoneId == null) {
            return null;
        }

        String resolvedTimezoneId = timezoneId.trim();
        validateTimezoneId(resolvedTimezoneId);
        return resolvedTimezoneId;
    }

    private void validateTimezoneId(String timezoneId) {
        try {
            ZoneId.of(timezoneId);
        } catch (DateTimeException e) {
            throw new PatientSettingsInvalidException();
        }
    }

    private int resolveExpiresInMinutes(PatientAccessLinkCreateReq request) {
        int expiresInMinutes = request == null
                ? PatientAccessLinkCreateReq.DEFAULT_EXPIRES_IN_MINUTES
                : request.resolvedExpiresInMinutes();

        if (expiresInMinutes < PatientAccessLinkCreateReq.MIN_EXPIRES_IN_MINUTES
                || expiresInMinutes > PatientAccessLinkCreateReq.MAX_EXPIRES_IN_MINUTES) {
            throw new PatientAccessLinkExpirationInvalidException();
        }
        return expiresInMinutes;
    }

    private GeneratedToken generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String rawToken = generateRawToken();
            String tokenHash = hashToken(rawToken);

            if (!patientAccessLinkRepository.existsByTokenHash(tokenHash)) {
                return new GeneratedToken(rawToken, tokenHash);
            }
        }
        throw new PatientAccessLinkTokenGenerationFailedException();
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PatientAccessLinkHashFailedException();
        }
    }

    private String buildMagicLink(String rawToken) {
        return UriComponentsBuilder.fromUriString(patientAccessLinkBaseUrl)
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private record GeneratedToken(String rawToken, String tokenHash) {
    }
}
