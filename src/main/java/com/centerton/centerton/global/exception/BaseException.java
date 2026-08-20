package com.centerton.centerton.global.exception;


import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final BaseResponseCode baseResponseCode;

    public BaseException(BaseResponseCode baseResponseCode) {
        this.baseResponseCode = baseResponseCode;
    }

    public BaseException(
            BaseResponseCode baseResponseCode,
            Throwable cause
    ) {
        super(cause);
        this.baseResponseCode = baseResponseCode;
    }
}
