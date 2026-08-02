# Backend

백엔드 레포지토리입니다.

## 주요 기능

- 환자별 사후관리 가이드 제공
- 증상 및 사진·영상 기반 사전 자료 제출
- 화상 상담 예약 및 상담 대기실 제공
- 실시간 통역 화상 상담
- 상담 기록 및 요약 확인
- AI 기반 비의료적 웰니스 안내와 문의 분류

## 팀 컨벤션

### 공통 용어

| 한글 | 영문 |
| --- | --- |
| 화상상담 | `Consultation` |
| 화상 상담 예약 | `ConsultationReservation` |
| 상담 대기실 | `ConsultationWaitingRoom` |
| 상담 기록 | `ConsultationRecord` |
| 상담 요약 | `ConsultationSummary` |
| 사후관리 | `Aftercare` |
| 환자 | `Patient` |
| 사전 제출 자료 | `PreConsultationSubmission` |

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스·인터페이스 | PascalCase | `ConsultationService`, `TranslationClient` |
| 메서드·변수 | camelCase | `createReservation`, `consultationReservation` |
| 상수·Enum 값 | UPPER_SNAKE_CASE | `MAX_MEMO_LENGTH`, `COMPLETED` |
| 패키지 | 소문자 | `consultation.reservation` |
| 요청 DTO | 이름 + `Req` | `PatientReq` |
| 응답 DTO | 이름 + `Res` | `PatientRes` |

메서드 이름은 `동사 + 대상` 형태로 작성합니다.

| 목적 | 접두어 |
| --- | --- |
| 데이터가 없을 수 있는 조회 | `find` |
| 데이터가 반드시 존재하는 조회 | `get` |
| 생성 | `create` |
| 수정 | `update` |
| 삭제 | `delete` |

### 코드 스타일

- 들여쓰기: 공백 4칸
- 한 줄 최대 길이: 120자
- 중괄호: 선언과 같은 줄에서 시작
- 파일 인코딩: UTF-8
- 파일 마지막: 빈 줄 1개

### 커밋 메시지

Gitmoji는 사용하지 않으며, 다음 형식으로 작성합니다.

```text
<type>: <작업 내용>
```

| Type | 설명 |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `docs` | 문서 수정 |
| `test` | 테스트 추가 및 수정 |
| `chore` | 설정, 의존성, 빌드 작업 |
| `style` | 코드 포맷 수정 |

### PR 규칙

PR 제목은 `[Type] 작업 내용` 형식으로 작성합니다.

```text
[Feat] 화상상담 예약 기능 구현
[Fix] 상담 대기실 입장 시간 오류 수정
[Refactor] 상담 정책 로직 분리
```

PR 본문에는 아래 내용을 포함합니다.

- 작업 개요
- 작업 내용
- 테스트 내용(API 동작 및 코드 빌드 여부)
- 관련 이슈
- 기타 사항
