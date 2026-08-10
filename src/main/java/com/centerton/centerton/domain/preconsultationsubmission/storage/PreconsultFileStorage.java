package com.centerton.centerton.domain.preconsultationsubmission.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface PreconsultFileStorage {

    StoredPreconsultFile store(MultipartFile file);

    Resource load(String storedFileName);

    void delete(String storedFileName);
}
