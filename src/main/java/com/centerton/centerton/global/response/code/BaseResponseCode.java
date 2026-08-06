package com.centerton.centerton.global.response.code;

public interface BaseResponseCode {
    String getCode();

    int getHttpStatus();

    String getMessage();
}
