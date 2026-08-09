package com.centerton.centerton.domain.aftercare.dto.response;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.entity.ProcedureRecord;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.PatientAllergy;
import com.centerton.centerton.domain.patient.entity.enums.Gender;

import java.time.LocalDate;
import java.util.List;

public record EmergencyMedicalReportRes(
        Long caseId,
        PatientInfo patient,
        ProcedureDetails procedure,
        MedicationAndAllergies medicationAndAllergies,
        EmergencyContacts emergencyContacts
) {

    public static EmergencyMedicalReportRes from(AftercareCase aftercareCase, List<PatientAllergy> allergies) {
        Patient patient = aftercareCase.getPatient();
        ProcedureRecord procedureRecord = aftercareCase.getProcedureRecord();

        return new EmergencyMedicalReportRes(
                aftercareCase.getCaseId(),
                PatientInfo.from(patient),
                ProcedureDetails.from(procedureRecord),
                MedicationAndAllergies.from(procedureRecord, allergies),
                EmergencyContacts.from(aftercareCase)
        );
    }

    public record PatientInfo(
            String name,
            String englishName,
            String displayName,
            LocalDate birthDate,
            String genderDisplayName
    ) {

        private static PatientInfo from(Patient patient) {
            String genderValue = patient.getGender() == null ? null : patient.getGender().getValue();
            String genderEnglishName = toGenderEnglishName(patient.getGender());

            return new PatientInfo(
                    patient.getName(),
                    patient.getEnglishName(),
                    withEnglishName(patient.getName(), patient.getEnglishName()),
                    patient.getBirthDate(),
                    withEnglishName(genderValue, genderEnglishName)
            );
        }
    }

    public record ProcedureDetails(
            LocalDate procedureDate,
            String procedureName,
            String procedureEnglishName,
            String procedureDisplayName,
            String materials
    ) {

        private static ProcedureDetails from(ProcedureRecord procedureRecord) {
            return new ProcedureDetails(
                    procedureRecord.getProcedureDate(),
                    procedureRecord.getProcedureName(),
                    procedureRecord.getProcedureEnglishName(),
                    withEnglishName(procedureRecord.getProcedureName(), procedureRecord.getProcedureEnglishName()),
                    procedureRecord.getMaterials()
            );
        }
    }

    public record MedicationAndAllergies(
            String medications,
            List<AllergyInfo> allergies
    ) {

        private static MedicationAndAllergies from(ProcedureRecord procedureRecord, List<PatientAllergy> allergies) {
            return new MedicationAndAllergies(
                    procedureRecord.getMedications(),
                    allergies.stream().map(AllergyInfo::from).toList()
            );
        }
    }

    public record AllergyInfo(
            Long allergyId,
            String allergenName,
            String allergenEnglishName,
            String allergenDisplayName
    ) {

        private static AllergyInfo from(PatientAllergy allergy) {
            return new AllergyInfo(
                    allergy.getAllergyId(),
                    allergy.getAllergenName(),
                    allergy.getAllergenEnglishName(),
                    withEnglishName(allergy.getAllergenName(), allergy.getAllergenEnglishName())
            );
        }
    }

    public record EmergencyContacts(
            String clinicPhoneNumber,
            String guardianPhoneNumber
    ) {

        private static EmergencyContacts from(AftercareCase aftercareCase) {
            return new EmergencyContacts(
                    toInternationalKoreanPhoneNumber(aftercareCase.getClinicPhoneNumber()),
                    toInternationalKoreanPhoneNumber(aftercareCase.getGuardianPhoneNumber())
            );
        }
    }

    private static String withEnglishName(String koreanName, String englishName) {
        if (!hasText(koreanName)) {
            return trimToNull(englishName);
        }
        if (!hasText(englishName)) {
            return koreanName.trim();
        }
        return koreanName.trim() + " (" + englishName.trim() + ")";
    }

    private static String toGenderEnglishName(Gender gender) {
        if (gender == null) {
            return null;
        }

        return switch (gender) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case OTHER -> "Other";
            case UNKNOWN -> "Unknown";
        };
    }

    private static String toInternationalKoreanPhoneNumber(String phoneNumber) {
        if (!hasText(phoneNumber)) {
            return null;
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return trimmed;
        }
        if (digits.startsWith("82")) {
            return "+82-" + formatKoreanSubscriberNumber(digits.substring(2));
        }
        if (digits.startsWith("0")) {
            return "+82-" + formatKoreanSubscriberNumber(digits.substring(1));
        }

        return trimmed;
    }

    private static String formatKoreanSubscriberNumber(String subscriberNumber) {
        if (subscriberNumber.length() <= 4) {
            return subscriberNumber;
        }
        if (subscriberNumber.startsWith("2")) {
            return "2-" + splitLastFour(subscriberNumber.substring(1));
        }
        if (subscriberNumber.length() >= 9) {
            return subscriberNumber.substring(0, 2) + "-" + splitLastFour(subscriberNumber.substring(2));
        }
        return splitLastFour(subscriberNumber);
    }

    private static String splitLastFour(String value) {
        if (value.length() <= 4) {
            return value;
        }

        int lastGroupStartIndex = value.length() - 4;
        return value.substring(0, lastGroupStartIndex) + "-" + value.substring(lastGroupStartIndex);
    }

    private static String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
