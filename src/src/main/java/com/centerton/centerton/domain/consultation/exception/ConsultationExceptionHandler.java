package com.centerton.centerton.domain.consultation.exception;

import com.centerton.centerton.domain.consultation.dto.response.ErrorRes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.centerton.centerton.domain.consultation")
public class ConsultationExceptionHandler {

    @ExceptionHandler(ConsultationException.class)
    public ResponseEntity<ErrorRes> handleConsultationException(
            ConsultationException exception
    ) {
        ConsultationErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorRes(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRes> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ResponseEntity
                .badRequest()
                .body(new ErrorRes("INVALID_REQUEST", message));
    }
}
