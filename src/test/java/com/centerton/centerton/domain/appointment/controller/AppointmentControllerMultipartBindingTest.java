package com.centerton.centerton.domain.appointment.controller;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentDetailRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentInfoRes;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.appointment.service.AppointmentService;
import com.centerton.centerton.domain.preconsultationsubmission.converter.SymptomCategoryConverter;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.global.jwt.PatientDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AppointmentControllerMultipartBindingTest {

    private static final Long PATIENT_ID = 1L;

    private AppointmentService appointmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        PatientDetails patientDetails = mock(PatientDetails.class);
        when(patientDetails.getPatientId()).thenReturn(PATIENT_ID);
        when(appointmentService.createAppointment(
                eq(PATIENT_ID),
                any(AppointmentCreateReq.class)
        )).thenReturn(new AppointmentDetailRes(
                101L,
                1L,
                201L,
                null,
                null,
                null,
                null,
                false,
                "UTC",
                AppointmentStatus.CONFIRMED
        ));

        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new SymptomCategoryConverter());

        mockMvc = standaloneSetup(
                new AppointmentController(appointmentService)
        )
                .setConversionService(conversionService)
                .setCustomArgumentResolvers(
                        authenticationPrincipalResolver(patientDetails)
                )
                .build();
    }

    @Test
    void bindsLegacySingleSymptomCategory() throws Exception {
        AppointmentCreateReq request = performAndCapture(
                "symptomCategory",
                "붓기"
        );

        assertThat(request.getSymptomCategory())
                .isEqualTo(SymptomCategory.SWELLING);
        assertThat(request.getSymptomCategories()).isEmpty();
    }

    @Test
    void bindsNewSingleSymptomCategory() throws Exception {
        AppointmentCreateReq request = performAndCapture(
                "symptomCategories",
                "붓기"
        );

        assertThat(request.getSymptomCategories())
                .containsExactly(SymptomCategory.SWELLING);
    }

    @Test
    void bindsRepeatedNewSymptomCategories() throws Exception {
        AppointmentCreateReq request = performAndCapture(
                "symptomCategories",
                "붓기",
                "멍"
        );

        assertThat(request.getSymptomCategories()).containsExactly(
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
    }

    @Test
    void getsAppointmentInfoWithoutMedicalStaffFields() throws Exception {
        when(appointmentService.getAppointmentInfo(PATIENT_ID, 101L))
                .thenReturn(new AppointmentInfoRes(
                        101L,
                        OffsetDateTime.parse("2026-07-30T14:00:00Z"),
                        OffsetDateTime.parse("2026-07-30T14:15:00Z"),
                        List.of(
                                SymptomCategory.SWELLING,
                                SymptomCategory.BRUISING
                        ),
                        "수술 부위가 붓고 멍이 심합니다."
                ));

        mockMvc.perform(get("/appointments/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").value(101))
                .andExpect(jsonPath("$.data.startsAt")
                        .value("2026-07-30T14:00:00Z"))
                .andExpect(jsonPath("$.data.endsAt")
                        .value("2026-07-30T14:15:00Z"))
                .andExpect(jsonPath("$.data.symptomCategories[0]")
                        .value("붓기"))
                .andExpect(jsonPath("$.data.symptomCategories[1]")
                        .value("멍"))
                .andExpect(jsonPath("$.data.symptomNote")
                        .value("수술 부위가 붓고 멍이 심합니다."))
                .andExpect(jsonPath("$.data.doctorName").doesNotExist())
                .andExpect(jsonPath("$.data.doctorId").doesNotExist());

        verify(appointmentService).getAppointmentInfo(PATIENT_ID, 101L);
    }

    private AppointmentCreateReq performAndCapture(
            String symptomParameter,
            String... symptomValues
    ) throws Exception {
        mockMvc.perform(multipart("/appointments")
                        .param("caseId", "1")
                        .param("slotId", "201")
                        .param(symptomParameter, symptomValues))
                .andExpect(status().isOk());

        ArgumentCaptor<AppointmentCreateReq> captor =
                ArgumentCaptor.forClass(AppointmentCreateReq.class);
        verify(appointmentService).createAppointment(
                eq(PATIENT_ID),
                captor.capture()
        );
        return captor.getValue();
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver(
            PatientDetails patientDetails
    ) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(
                        AuthenticationPrincipal.class
                );
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return patientDetails;
            }
        };
    }
}
