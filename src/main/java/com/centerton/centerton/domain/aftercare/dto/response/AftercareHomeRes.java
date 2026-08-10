package com.centerton.centerton.domain.aftercare.dto.response;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.entity.ProcedureRecord;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentDetailRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentLookupRes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AftercareHomeRes(
        Long caseId,
        AftercareProgress aftercareProgress,
        ProcedureSummary procedure,
        ConsultationAppointment consultationAppointment
) {

    public static AftercareHomeRes from(
            AftercareCase aftercareCase,
            LocalDate referenceDate,
            AppointmentLookupRes appointmentLookup
    ) {
        int currentDay = aftercareCase.calculateCurrentDay(referenceDate);

        return new AftercareHomeRes(
                aftercareCase.getCaseId(),
                AftercareProgress.from(aftercareCase, currentDay),
                ProcedureSummary.from(aftercareCase.getProcedureRecord()),
                ConsultationAppointment.from(appointmentLookup)
        );
    }

    public record AftercareProgress(
            Integer elapsedDays,
            Integer totalCareDays
    ) {

        private static AftercareProgress from(AftercareCase aftercareCase, int currentDay) {
            Integer totalCareDays = aftercareCase.getTotalCareDays();
            int elapsedDays = calculateElapsedDays(currentDay, totalCareDays);

            return new AftercareProgress(
                    elapsedDays,
                    totalCareDays
            );
        }

        private static int calculateElapsedDays(int currentDay, Integer totalCareDays) {
            if (totalCareDays == null || totalCareDays <= 0) {
                return currentDay;
            }
            return Math.min(currentDay, totalCareDays);
        }
    }

    public record ProcedureSummary(
            String procedureName,
            LocalDate procedureDate
    ) {

        private static ProcedureSummary from(ProcedureRecord procedureRecord) {
            return new ProcedureSummary(
                    procedureRecord.getProcedureName(),
                    procedureRecord.getProcedureDate()
            );
        }
    }

    public record ConsultationAppointment(
            Long appointmentId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            Boolean canEnterWaitingRoom,
            String timezoneId
    ) {

        private static ConsultationAppointment from(AppointmentLookupRes appointmentLookup) {
            if (appointmentLookup == null || !appointmentLookup.hasAppointment()) {
                return null;
            }

            AppointmentDetailRes appointment = appointmentLookup.appointment();

            return new ConsultationAppointment(
                    appointment.appointmentId(),
                    appointment.startsAt(),
                    appointment.endsAt(),
                    appointment.canEnterWaitingRoom(),
                    appointment.timezoneId()
            );
        }
    }
}
