package com.centerton.centerton.domain.appointment.controller;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentCancelReq;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentChangeReq;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentDetailRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentLookupRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableDateRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableSlotListRes;
import com.centerton.centerton.domain.appointment.service.AppointmentService;
import com.centerton.centerton.global.jwt.PatientDetails;
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public SuccessResponse<AppointmentLookupRes> getAppointment(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @RequestParam @Positive Long caseId
    ) {
        return SuccessResponse.from(
                appointmentService.getAppointment(
                        patientDetails.getPatientId(),
                        caseId
                )
        );
    }

    @GetMapping("/available-dates")
    public SuccessResponse<List<AvailableDateRes>> getAvailableDates(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        return SuccessResponse.from(
                appointmentService.getAvailableDates(
                        patientDetails.getPatientId(),
                        year,
                        month
                )
        );
    }

    @GetMapping("/available-slots")
    public SuccessResponse<AvailableSlotListRes> getAvailableSlots(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return SuccessResponse.from(
                appointmentService.getAvailableSlots(
                        patientDetails.getPatientId(),
                        date
                )
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<AppointmentDetailRes> createAppointment(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @Valid @ModelAttribute AppointmentCreateReq request
    ) {
        return SuccessResponse.created(
                appointmentService.createAppointment(
                        patientDetails.getPatientId(),
                        request
                )
        );
    }

    @PatchMapping("/{appointmentId}")
    public SuccessResponse<AppointmentDetailRes> changeAppointment(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @PathVariable @Positive Long appointmentId,
            @Valid @RequestBody AppointmentChangeReq request
    ) {
        return SuccessResponse.from(
                appointmentService.changeAppointment(
                        patientDetails.getPatientId(),
                        appointmentId,
                        request
                )
        );
    }

    @DeleteMapping("/{appointmentId}")
    public SuccessResponse<Void> cancelAppointment(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @PathVariable @Positive Long appointmentId,
            @Valid @RequestBody AppointmentCancelReq request
    ) {
        appointmentService.cancelAppointment(
                patientDetails.getPatientId(),
                appointmentId,
                request
        );
        return SuccessResponse.empty();
    }
}
