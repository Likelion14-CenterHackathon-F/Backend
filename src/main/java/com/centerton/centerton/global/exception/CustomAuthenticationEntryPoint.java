package com.centerton.centerton.global.exception;

import com.centerton.centerton.global.response.ErrorResponse;
import com.centerton.centerton.global.response.code.ErrorResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        String exception = (String) request.getAttribute("exception");
        ErrorResponseCode errorCode;

        if ("EXPIRED_TOKEN".equals(exception)) {
            errorCode = ErrorResponseCode.EXPIRED_TOKEN;
        } else if ("INVALID_TOKEN".equals(exception)) {
            errorCode = ErrorResponseCode.INVALID_TOKEN;
        } else {
            errorCode = ErrorResponseCode.UNAUTHORIZED_REQUEST;
        }

        setResponse(response, errorCode);
    }

    private void setResponse(HttpServletResponse response, ErrorResponseCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse<?> body = ErrorResponse.from(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
