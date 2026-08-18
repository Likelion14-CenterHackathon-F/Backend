package com.centerton.centerton.global.response;


import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Getter
@ToString
@RequiredArgsConstructor
public class BaseResponse {
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final String timestamp = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS)
            .toString();

    public static BaseResponse of(Boolean isSuccess, BaseResponseCode baseResponseCode) {
        return new BaseResponse(isSuccess, baseResponseCode.getCode(), baseResponseCode.getMessage());
    }

    public static BaseResponse of(Boolean isSuccess, BaseResponseCode baseResponseCode, String message) {
        return new BaseResponse(isSuccess, baseResponseCode.getCode(), message);
    }

    public static BaseResponse of(Boolean isSuccess, String code, String message) {
        return new BaseResponse(isSuccess, code, message);
    }
}
