package com.centerton.centerton.domain.appointment.dto.response;

public record AppointmentLookupRes(
        boolean hasAppointment,
        AppointmentDetailRes appointment
) {

    public static AppointmentLookupRes none() {
        return new AppointmentLookupRes(false, null);
    }

    public static AppointmentLookupRes of(AppointmentDetailRes appointment) {
        return new AppointmentLookupRes(true, appointment);
    }
}
