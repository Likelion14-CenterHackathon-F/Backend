package com.centerton.centerton.domain.consultation.service;

import org.springframework.stereotype.Component;

@Component
public class ConsultationJoinPolicy {

    public void validateJoin(Long appointmentId) {
        /*
         * TODO 예약 도메인 구현 후 아래 내용을 검사해야 한다.
         *
         * 1. 로그인 사용자가 예약 당사자인지
         * 2. 예약 상태가 확정 상태인지
         * 3. 예약이 취소 또는 완료되지 않았는지
         * 4. 예약 시각 10분 전부터 예약 시각 20분 후까지인지
         *
         * 현재 화상상담 도메인만 먼저 개발하기 때문에
         * 이 클래스는 예약 도메인 연결 지점으로 남겨둔다.
         */
    }
}
