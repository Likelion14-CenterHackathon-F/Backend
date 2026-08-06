package com.centerton.centerton.global.jwt;

import com.centerton.centerton.global.jwt.exception.TokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final JwtParser parser;

    public Claims parseClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public Long getPatientId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public void validateTokenOrThrow(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidException();
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}
