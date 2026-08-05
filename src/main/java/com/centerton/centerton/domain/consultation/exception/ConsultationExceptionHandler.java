package com.centerton.centerton.domain.consultation.exception;

import com.centerton.centerton.domain.consultation.dto.response.ErrorRes;
import com.centerton.centerton.global.response.code.BaseResponseCode;
import com.centerton.centerton.global.response.code.ErrorResponseCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.centerton.centerton.domain.consultation"
)
public class ConsultationExceptionHandler {

    @ExceptionHandler(ConsultationException.class)
    public ResponseEntity<ErrorRes> handleConsultationException(
            ConsultationException exception
    ) {
        BaseResponseCode errorCode =
                exception.getBaseResponseCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorRes(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRes> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        ErrorResponseCode errorCode =
                ErrorResponseCode.INVALID_HTTP_MESSAGE_PARAMETER;

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError ->
                        fieldError.getField()
                                + ": "
                                + fieldError.getDefaultMessage()
                )
                .orElse(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorRes(
                        errorCode.getCode(),
                        message
                ));
    }
}