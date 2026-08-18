package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.dto.response.AiChatDownloadImage;
import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.repository.AiChatMessageRepository;
import com.centerton.centerton.domain.aichat.repository.AiChatRoomRepository;
import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatQueryServiceImageAccessTest {

    @Mock
    private AiChatRoomRepository chatRoomRepository;
    @Mock
    private AiChatMessageRepository chatMessageRepository;
    @Mock
    private AiChatImageStorage imageStorage;

    private AiChatQueryService service;

    @BeforeEach
    void setUp() {
        service = new AiChatQueryService(
                chatRoomRepository,
                chatMessageRepository,
                imageStorage
        );
    }

    @Test
    void loadsImageOnlyAfterOwnershipQuerySucceeds() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{1});
        when(imageStorage.resolveDisplayImageUrl("stored.jpg"))
                .thenReturn("/api/ai-chats/images/stored.jpg");
        when(chatMessageRepository.findAccessibleImageMessage(
                "/api/ai-chats/images/stored.jpg",
                1L
        )).thenReturn(Optional.of(org.mockito.Mockito.mock(
                AiChatMessage.class
        )));
        when(imageStorage.resolveContentType("stored.jpg"))
                .thenReturn("image/jpeg");
        when(imageStorage.load("stored.jpg")).thenReturn(resource);

        AiChatDownloadImage result = service.getImage(1L, "stored.jpg");

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.resource()).isSameAs(resource);
        verify(chatMessageRepository).findAccessibleImageMessage(
                "/api/ai-chats/images/stored.jpg",
                1L
        );
    }

    @Test
    void doesNotLoadImageWhenOwnershipQueryFails() {
        when(imageStorage.resolveDisplayImageUrl("stored.jpg"))
                .thenReturn("/api/ai-chats/images/stored.jpg");
        when(chatMessageRepository.findAccessibleImageMessage(
                "/api/ai-chats/images/stored.jpg",
                2L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getImage(2L, "stored.jpg"))
                .isInstanceOfSatisfying(
                        BaseException.class,
                        exception -> assertThat(exception.getBaseResponseCode())
                                .isEqualTo(AiChatErrorCode.IMAGE_NOT_FOUND)
                );

        verify(imageStorage, never()).load("stored.jpg");
    }
}
