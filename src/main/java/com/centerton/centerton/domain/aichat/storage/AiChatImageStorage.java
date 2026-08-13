package com.centerton.centerton.domain.aichat.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AiChatImageStorage {

    StoredAiChatImage store(MultipartFile image);

    Resource load(String storedFileName);

    String resolveDisplayImageUrl(String storedFileName);

    String resolveAnalysisImageUrl(String storedFileName);

    String resolveContentType(String storedFileName);

    void delete(String storedFileName);
}
