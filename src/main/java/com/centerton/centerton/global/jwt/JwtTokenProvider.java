package com.centerton.centerton.global.jwt;

import com.centerton.centerton.domain.patient.entity.enums.Language;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_LANGUAGE = "language";
    private static final String ROLE_PATIENT = "PATIENT";

    private final Key key;

    @Value("${jwt.access.expiration}")
    private long accessExpirationMillis;

    public String createPatientAccessToken(Long patientId) {
        return createPatientAccessToken(patientId, null);
    }

    public String createPatientAccessToken(Long patientId, Language language) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpirationMillis);

        JwtBuilder builder = Jwts.builder()
                .subject(patientId.toString())
                .claim(CLAIM_ROLE, ROLE_PATIENT)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key);

        if (language != null) {
            builder.claim(CLAIM_LANGUAGE, language.name());
        }

        return builder.compact();
    }
}
