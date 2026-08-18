package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.request.AiChatSymptomInquiryReq;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.safety.AiChatEmergencyRuleService;
import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.domain.aichat.storage.StoredAiChatImage;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceStorageTest {

    @Mock
    private AiChatImageStorage imageStorage;
    @Mock
    private AiChatAnswerService answerService;
    @Mock
    private AiChatTransactionService transactionService;
    @Mock
    private AiChatQueryService queryService;
    @Mock
    private AiChatResponseTranslator responseTranslator;
    @Mock
    private AiChatEmergencyRuleService emergencyRuleService;
    @Mock
    private MultipartFile image;

    @InjectMocks
    private AiChatService service;

    @Test
    void deletesImageWhenUserMessageIsNotSaved() {
        AiChatSymptomInquiryReq request = new AiChatSymptomInquiryReq();
        request.setQuestion("붓기가 심합니다");
        request.setImage(image);
        StoredAiChatImage storedImage = new StoredAiChatImage(
                "stored.jpg",
                "/api/ai-chats/images/stored.jpg"
        );
        when(image.isEmpty()).thenReturn(false);
        when(imageStorage.store(image)).thenReturn(storedImage);
        when(transactionService.saveUserMessage(
                eq(1L),
                eq(null),
                eq("붓기가 심합니다"),
                eq("/api/ai-chats/images/stored.jpg"),
                any()
        )).thenThrow(new BaseException(AiChatErrorCode.CHAT_ROOM_NOT_FOUND));

        assertThatThrownBy(() -> service.createSymptomInquiry(
                1L,
                Language.KO,
                request
        )).isInstanceOf(BaseException.class);

        verify(imageStorage).delete("stored.jpg");
    }
}
