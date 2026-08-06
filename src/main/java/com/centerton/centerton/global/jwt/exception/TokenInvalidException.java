package com.centerton.centerton.global.jwt.exception;

import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.response.code.ErrorResponseCode;

public class TokenInvalidException extends BaseException {

    public TokenInvalidException() {
        super(ErrorResponseCode.INVALID_TOKEN);
    }
}
