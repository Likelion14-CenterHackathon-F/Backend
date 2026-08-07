package com.centerton.centerton.domain.patient.entity;

import com.centerton.centerton.domain.patient.entity.enums.Language;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "patients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Patient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "email")
    private String email;

    // ISO 국가 코드로 저장 (ex: KR, US, JP)
    @Column(name = "nationality", length = 2)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 2)
    private Language language;

    @Column(name = "timezone_id")
    private String timezoneId;

    public void updateSettings(Language language, String nationality, String timezoneId) {
        if (language != null) {
            this.language = language;
        }
        if (nationality != null) {
            this.nationality = nationality;
        }
        if (timezoneId != null) {
            this.timezoneId = timezoneId;
        }
    }
}
