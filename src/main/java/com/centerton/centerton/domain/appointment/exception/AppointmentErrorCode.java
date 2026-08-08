package com.centerton.centerton.domain.appointment.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.centerton.centerton.global.constant.StaticValue.CONFLICT;
import static com.centerton.centerton.global.constant.StaticValue.FORBIDDEN;
import static com.centerton.centerton.global.constant.StaticValue.NOT_FOUND;

@Getter
@AllArgsConstructor
public enum AppointmentErrorCode implements BaseResponseCode {

    APPOINTMENT_NOT_FOUND(
            "APPOINTMENT_404_1",
            NOT_FOUND,
            "화상상담 예약을 찾을 수 없습니다."
    ),

    RESERVATION_SLOT_NOT_FOUND(
            "APPOINTMENT_404_2",
            NOT_FOUND,
            "예약 시간 슬롯을 찾을 수 없습니다."
    ),

    RESERVATION_SLOT_UNAVAILABLE(
            "APPOINTMENT_409_1",
            CONFLICT,
            "이미 마감되었거나 예약할 수 없는 시간 슬롯입니다."
    ),

    ACTIVE_APPOINTMENT_ALREADY_EXISTS(
            "APPOINTMENT_409_2",
            CONFLICT,
            "이미 활성화된 화상상담 예약이 있습니다."
    ),

    APPOINTMENT_ALREADY_STARTED(
            "APPOINTMENT_409_3",
            CONFLICT,
            "이미 시작된 예약은 변경하거나 취소할 수 없습니다."
    ),

    APPOINTMENT_JOIN_NOT_ALLOWED(
            "APPOINTMENT_403_1",
            FORBIDDEN,
            "현재는 상담 대기실에 입장할 수 있는 시간이 아닙니다."
    );

    private final String code;
    private final int httpStatus;
    private final String message;
}
