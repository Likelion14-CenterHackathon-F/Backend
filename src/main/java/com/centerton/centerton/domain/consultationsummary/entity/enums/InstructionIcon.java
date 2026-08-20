package com.centerton.centerton.domain.consultationsummary.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 의료진 지시사항 카드에 표시되는 아이콘.
 *
 * <p>code 1~6 은 클라이언트 아이콘 시안의 왼쪽부터 순서와 일치하고,
 * 7 은 어느 아이콘에도 해당하지 않는 지시사항을 위한 기본값이다.
 * 응답에는 enum 이름이 아니라 이 번호가 내려간다.
 */
@Getter
@AllArgsConstructor
public enum InstructionIcon {

    CLEANSING(1, "세안, 샤워, 목욕, 머리 감기 등 씻는 행위에 대한 지시"),
    COLD_COMPRESS(2, "냉찜질, 온찜질, 붓기 완화를 위한 찜질에 대한 지시"),
    EXERCISE(3, "운동, 활동량, 무리한 움직임 제한에 대한 지시"),
    SUN_PROTECTION(4, "자외선 차단, 햇빛 노출 주의, 선크림에 대한 지시"),
    MEDICATION(5, "처방약 복용, 연고 도포 등 약에 대한 지시"),
    HYDRATION(6, "물 마시기, 수분 섭취, 탈수 예방에 대한 지시"),
    ETC(7, "위 여섯 항목 중 어디에도 해당하지 않는 지시. "
            + "예: 경과 관찰, 다음 상담 예약, 휴식, 식단 주의");

    private final int code;
    private final String description;

    /**
     * 번호를 아이콘으로 바꾼다.
     *
     * <p>Gemini 가 번호를 빠뜨리거나 목록에 없는 번호를 반환할 수 있으므로
     * 알 수 없는 값은 모두 {@link #ETC} 로 떨어뜨린다.
     * 응답의 icon 이 비지 않도록 하는 것이 이 fallback 의 목적이다.
     */
    public static InstructionIcon fromCode(Integer code) {
        if (code == null) {
            return ETC;
        }

        return Arrays.stream(values())
                .filter(icon -> icon.code == code)
                .findFirst()
                .orElse(ETC);
    }

    /**
     * Gemini 프롬프트에 넣을 아이콘 목록.
     *
     * <p>아이콘이 추가되면 프롬프트도 함께 바뀌도록 enum 에서 만든다.
     */
    public static String promptGuide() {
        return Arrays.stream(values())
                .map(icon -> "    " + icon.code + ": " + icon.description)
                .collect(Collectors.joining("\n"));
    }
}
