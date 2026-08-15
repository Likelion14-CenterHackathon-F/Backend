package com.centerton.centerton.domain.appointment.dto.request;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentCreateReq {

    @NotNull
    @Positive
    private Long caseId;

    @NotNull
    @Positive
    private Long slotId;

    private SymptomCategory symptomCategory;

    private String symptomNote;

    private List<MultipartFile> files = new ArrayList<>();
}
