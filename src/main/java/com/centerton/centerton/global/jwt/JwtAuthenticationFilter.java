package com.centerton.centerton.global.jwt;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.jwt.exception.TokenInvalidException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = jwtTokenUtil.resolveToken(request);

            try {
                if (token != null) {
                    jwtTokenUtil.validateTokenOrThrow(token);

                    Authentication authentication = getAuthentication(request, token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute("exception", "EXPIRED_TOKEN");
            } catch (TokenInvalidException e) {
                request.setAttribute("exception", "INVALID_TOKEN");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Authentication getAuthentication(HttpServletRequest request, String token) {
        Long patientId = jwtTokenUtil.getPatientId(token);
        Language language = jwtTokenUtil.getPatientLanguage(token);

        Patient patient = Patient.builder()
                .id(patientId)
                .build();

        PatientDetails patientDetails = new PatientDetails(patient, language);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(patientDetails, null, patientDetails.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return authentication;
    }
}
