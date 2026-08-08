package com.centerton.centerton.domain.preconsultationsubmission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "file_asset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    public static FileAsset create(
            String fileUrl,
            Long submissionId
    ) {
        return new FileAsset(
                null,
                fileUrl,
                submissionId
        );
    }
}
