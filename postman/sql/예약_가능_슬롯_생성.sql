-- 화상상담 예약 API 테스트용 예약 가능 슬롯 생성 스크립트
-- 대상 DB: MySQL 8+
--
-- 예약 가능 날짜는 별도 테이블에 저장하지 않습니다.
-- AppointmentService가 reservation_slots의 미래 가용 슬롯을 날짜별로 집계합니다.
-- 애플리케이션은 DB 날짜·시간을 UTC로 취급하므로 한국 시간(KST) 입력값을
-- UTC로 변환해 starts_at과 ends_at에 저장합니다.

-- 기본값: 한국 시간 기준 내일
-- 특정 날짜를 사용하려면 아래 값을 '2026-08-10'처럼 바꿉니다.
SET @target_date_kst = DATE_ADD(
    DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR)),
    INTERVAL 1 DAY
);

SET @slot_duration_minutes = 30;

START TRANSACTION;

INSERT INTO reservation_slots (
    starts_at,
    ends_at,
    availability
)
SELECT
    candidate.starts_at_utc,
    DATE_ADD(
        candidate.starts_at_utc,
        INTERVAL @slot_duration_minutes MINUTE
    ),
    TRUE
FROM (
    SELECT
        DATE_SUB(
            TIMESTAMP(@target_date_kst, slot_time.start_time_kst),
            INTERVAL 9 HOUR
        ) AS starts_at_utc
    FROM (
        SELECT CAST('10:00:00' AS TIME) AS start_time_kst
        UNION ALL
        SELECT CAST('11:00:00' AS TIME)
        UNION ALL
        SELECT CAST('14:00:00' AS TIME)
        UNION ALL
        SELECT CAST('15:00:00' AS TIME)
    ) AS slot_time
) AS candidate
WHERE candidate.starts_at_utc > UTC_TIMESTAMP()
  AND NOT EXISTS (
      SELECT 1
      FROM reservation_slots AS existing_slot
      WHERE existing_slot.starts_at = candidate.starts_at_utc
  )
ORDER BY candidate.starts_at_utc;

SELECT ROW_COUNT() AS inserted_slot_count;

COMMIT;

-- 저장 결과 확인: UTC 저장값과 API에서 한국 시간 환자에게 보일 값을 함께 조회합니다.
SELECT
    slot_id,
    starts_at AS starts_at_utc,
    ends_at AS ends_at_utc,
    DATE_ADD(starts_at, INTERVAL 9 HOUR) AS starts_at_kst,
    DATE_ADD(ends_at, INTERVAL 9 HOUR) AS ends_at_kst,
    availability
FROM reservation_slots
WHERE starts_at >= DATE_SUB(
        TIMESTAMP(@target_date_kst, '00:00:00'),
        INTERVAL 9 HOUR
    )
  AND starts_at < DATE_SUB(
        TIMESTAMP(DATE_ADD(@target_date_kst, INTERVAL 1 DAY), '00:00:00'),
        INTERVAL 9 HOUR
    )
ORDER BY starts_at;
