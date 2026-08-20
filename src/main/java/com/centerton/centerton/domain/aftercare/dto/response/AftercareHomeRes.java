package com.centerton.centerton.domain.aftercare.dto.response;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.entity.ProcedureRecord;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentHomeSummary;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AftercareHomeRes(
        Long caseId,
        String patientName,
        AftercareProgress aftercareProgress,
        ProcedureSummary procedure,
        ConsultationAppointment consultationAppointment
) {

    public static AftercareHomeRes from(
            AftercareCase aftercareCase,
            LocalDate referenceDate,
            AppointmentHomeSummary appointment
    ) {
        int currentDay = aftercareCase.calculateCurrentDay(referenceDate);

        return new AftercareHomeRes(
                aftercareCase.getCaseId(),
                resolvePatientName(aftercareCase),
                AftercareProgress.from(aftercareCase, currentDay),
                ProcedureSummary.from(aftercareCase.getProcedureRecord()),
                ConsultationAppointment.from(appointment)
        );
    }

    private static String resolvePatientName(AftercareCase aftercareCase) {
        var patient = aftercareCase.getPatient();
        if ("KR".equals(patient.getNationality())) {
            return patient.getName();
        }
        return patient.getEnglishName() != null ? patient.getEnglishName() : patient.getName();
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
            OffsetDateTime startsAt
    ) {

        private static ConsultationAppointment from(AppointmentHomeSummary appointment) {
            if (appointment == null) {
                return null;
            }

            return new ConsultationAppointment(
                    appointment.appointmentId(),
                    appointment.startsAt()
            );
        }
    }
}
