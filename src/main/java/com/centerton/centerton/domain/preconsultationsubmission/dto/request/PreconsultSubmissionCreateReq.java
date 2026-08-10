package com.centerton.centerton.domain.preconsultationsubmission.dto.request;

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
public class PreconsultSubmissionCreateReq {

    @NotNull
    @Positive
    private Long appointmentId;

    private String symptomNote;

    private List<MultipartFile> files = new ArrayList<>();
}
