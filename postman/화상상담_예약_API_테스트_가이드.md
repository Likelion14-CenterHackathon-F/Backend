# 화상상담 예약 API Postman 테스트 가이드

## 1. 구성 파일

- `화상상담_예약_API.postman_collection.json`: 요청과 자동 검증 스크립트
- `화상상담_예약_API.postman_environment.json`: 로컬 실행용 환경변수

두 파일을 Postman으로 가져온 뒤 `화상상담 예약 API - 로컬` 환경을 선택합니다.

## 2. 사전 조건

1. 서버가 `http://localhost:8080/api`에서 실행 중이어야 합니다.
2. `patientId`와 `patientBirthDate`가 일치하는 환자가 DB에 존재해야 합니다.
3. 테스트 대상 월에 현재 시각보다 미래인 예약 슬롯이 최소 2개 있어야 합니다.
4. 해당 환자와 `caseId` 조합에는 활성 예약이 없어야 합니다.
5. 슬롯의 `starts_at`, `ends_at`은 UTC 기준으로 저장되어 있어야 합니다.

기본 URL이나 데이터가 다르면 환경변수를 수정합니다. `testYear`와 `testMonth`를 비워 두면 실행 시점의 연월을 자동 사용합니다. 다른 월을 테스트할 때는 두 값을 직접 입력합니다.

## 3. 권장 실행 순서

Collection Runner에서 다음 폴더를 순서대로 실행합니다.

1. `00. 인증 준비`
2. `01. 예약 가능 일정 조회`
3. `02. 예약 생명주기 및 충돌`
4. `03. 인증 및 입력값 검증`

위 흐름에서 다음 변수가 자동으로 저장됩니다.

| 변수 | 저장 시점 | 용도 |
| --- | --- | --- |
| `accessLinkToken` | 접근 링크 생성 | 환자 인증 |
| `accessToken` | 접근 링크 검증 | Bearer 인증 |
| `appointmentDate` | 예약 가능 날짜 조회 | 슬롯 조회 |
| `slotId` | 예약 가능 시간 조회 | 최초 예약 |
| `alternateSlotId` | 예약 가능 시간 조회 | 예약 변경 |
| `appointmentId` | 예약 생성 | 조회·변경·취소 |

`02. 예약 생명주기 및 충돌`은 생성한 예약을 마지막에 취소하므로 반복 실행할 수 있습니다. 중간에 실행이 중단되면 DB에 활성 예약이 남을 수 있으므로 예약을 취소하거나 `caseId`를 바꾼 후 다시 실행합니다.

## 4. 자동 테스트 시나리오

### 정상 흐름

| 순서 | 시나리오 | 기대 결과 |
| --- | --- | --- |
| 1 | 접근 링크 생성 및 검증 | `GLOBAL_201`, `GLOBAL_200`; JWT 저장 |
| 2 | 예약 가능 날짜 조회 | 날짜 오름차순, `availableCount > 0` |
| 3 | 예약 가능 슬롯 조회 | 슬롯 시간 오름차순, 가용 슬롯 2개 자동 선택 |
| 4 | 예약 전 단건 조회 | `hasAppointment=false` |
| 5 | 예약 생성 | HTTP 201, `APPOINTMENT` 식별자와 시간 필드 검증 |
| 6 | 동일 슬롯으로 변경 | HTTP 200, 같은 예약·슬롯 유지(멱등성) |
| 7 | 다른 슬롯으로 변경 | HTTP 200, `alternateSlotId` 반영 |
| 8 | 예약 단건 재조회 | 변경 결과와 일치 |
| 9 | 예약 취소 | HTTP 200, `data=null` |
| 10 | 취소 후 재조회 | `hasAppointment=false` |

### 충돌 및 오류 흐름

| 시나리오 | 기대 HTTP | 기대 코드 |
| --- | ---: | --- |
| 동일 환자·케이스에 활성 예약 중복 생성 | 409 | `APPOINTMENT_409_2` |
| 이미 예약된 슬롯을 다른 케이스로 예약 | 409 | `APPOINTMENT_409_1` |
| 존재하지 않는 슬롯으로 변경/생성 | 404 | `APPOINTMENT_404_2` |
| 취소된 예약을 다시 취소 | 404 | `APPOINTMENT_404_1` |
| 인증 헤더 누락 | 401 | `GLOBAL_401_1` |
| 잘못된 JWT | 401 | `GLOBAL_401_3` |
| 허용 범위를 벗어난 월 | 400 | `GLOBAL_400_1` 또는 검증 정책 코드 |
| 잘못된 날짜 형식 | 400 | `GLOBAL_400_2` |
| `slotId=0` 요청 바디 | 400 | `GLOBAL_400_2` |

월 범위 검증이 HTTP 500으로 응답한다면 `HandlerMethodValidationException` 또는 `ConstraintViolationException`의 전역 예외 매핑이 빠진 것입니다. 해당 요청은 의도적으로 이 회귀를 검출합니다.

## 5. 선택 실행 시나리오

`04. 시간·권한 경계값`과 `05. 동시성`은 별도 데이터 준비가 필요합니다. 관련 환경변수가 비어 있으면 요청이 자동으로 건너뛰어집니다.

| 변수 | 필요한 데이터 |
| --- | --- |
| `pastSlotId` | 시작 시각이 현재 이하인 미예약 슬롯 |
| `startedAppointmentId` | 상담 세션이 이미 생성된 본인 예약 |
| `outsideWindowAppointmentId` | 입장 가능 시간(시작 10분 전~시작 20분 후) 밖의 본인 예약 |
| `otherPatientAccessToken` | 다른 환자의 정상 JWT |
| `foreignCaseId` | 첫 번째 환자에게만 예약이 존재하는 케이스 ID |
| `raceCaseId`, `raceSlotId` | 활성 예약이 없는 케이스와 미래의 가용 슬롯 |

동시성 요청은 같은 슬롯에 두 POST 요청을 거의 동시에 보냅니다. 정상 결과는 HTTP 201 한 건과 HTTP 409 한 건이며, 성공 예약은 테스트 후 직접 취소해야 합니다.

## 6. 실패 결과 해석

- 상태 코드 실패: 컨트롤러 매핑, 인증 또는 예외 처리부터 확인합니다.
- 응답 코드 실패: `ErrorResponseCode`, `AppointmentErrorCode` 매핑을 확인합니다.
- 날짜/시간 실패: 환자의 `timezoneId`, DB UTC 저장 여부, `AppointmentTimePolicy`를 확인합니다.
- 중복 예약 실패: 환자 행 잠금과 `uk_appointment_slot` 유니크 제약을 확인합니다.
- 타 환자 조회가 성공함: 예약 조회 조건에 `patientId`가 포함되었는지 확인합니다.

Postman Console에는 각 요청의 응답과 자동 저장된 변수를 출력할 수 있으며, Collection Runner의 실패 항목을 회귀 테스트 결과로 사용하면 됩니다.
