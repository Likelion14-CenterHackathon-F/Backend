package com.centerton.centerton.domain.aichat.controller;

import com.centerton.centerton.domain.aichat.dto.request.AiChatSymptomInquiryReq;
import com.centerton.centerton.domain.aichat.dto.response.AiChatDownloadImage;
import com.centerton.centerton.domain.aichat.dto.response.AiChatRoomListRes;
import com.centerton.centerton.domain.aichat.dto.response.AiChatSymptomInquiryRes;
import com.centerton.centerton.domain.aichat.service.AiChatService;
import com.centerton.centerton.global.jwt.PatientDetails;
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-chats")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping(
            value = "/messages",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public SuccessResponse<AiChatSymptomInquiryRes> createSymptomInquiry(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @Valid @ModelAttribute AiChatSymptomInquiryReq request
    ) {
        return SuccessResponse.created(
                aiChatService.createSymptomInquiry(
                        patientDetails.getPatientId(),
                        request
                )
        );
    }

    @GetMapping("/rooms")
    public SuccessResponse<List<AiChatRoomListRes>> getChatRooms(
            @AuthenticationPrincipal PatientDetails patientDetails
    ) {
        return SuccessResponse.from(
                aiChatService.getChatRooms(patientDetails.getPatientId())
        );
    }

    @GetMapping("/images/{storedFileName:.+}")
    public ResponseEntity<Resource> getImage(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @PathVariable String storedFileName
    ) {
        AiChatDownloadImage image = aiChatService.getImage(
                patientDetails.getPatientId(),
                storedFileName
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        image.storedFileName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
