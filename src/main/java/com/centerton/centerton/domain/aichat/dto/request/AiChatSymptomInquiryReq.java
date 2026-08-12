package com.centerton.centerton.domain.aichat.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class AiChatSymptomInquiryReq {

    @Positive
    private Long roomId;

    private String question;

    private MultipartFile image;
}
