package com.centerton.centerton.global.util;

public final class PhoneNumberFormatter {

    private PhoneNumberFormatter() {
    }

    public static String toInternationalKoreanPhoneNumber(String phoneNumber) {
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
