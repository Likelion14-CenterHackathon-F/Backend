# 🏥 Backend — API Server

> **외국인 환자를 위한 사후관리·화상상담 플랫폼의 핵심 백엔드입니다.**

환자 인증, 화상 상담 예약·진행, 사후관리 가이드, AI 채팅, 상담 요약까지
모바일 앱이 필요로 하는 모든 비즈니스 로직과 데이터를 처리합니다.

---

## 🔹 주요 기능

| 기능 | 설명 |
|------|------|
| 🔐 환자 인증 | 링크 기반 토큰 인증 (JWT) |
| 📅 화상 상담 예약 | 가용 슬롯 조회, 예약 생성·수정·취소 |
| 📹 실시간 화상 상담 | Agora RTC 토큰 발급, STT 자막, 세션 관리 |
| 📋 상담 요약 | Gemini + DeepL 기반 상담 요약·지시사항 생성 |
| 🩺 사후관리 | 회복 단계별 가이드, 응급의료 리포트 |
| 🤖 AI 채팅 | RAG 기반 사후관리 문의 답변 (FastAPI 서버 연동) |
| 📎 사전 자료 제출 | 증상 사진·영상 파일 업로드 (Local / S3) |

---

## 🚀 기술 스택

- **Language**: Java 21

  <img src="https://img.shields.io/badge/Java%2021-007396?style=flat-square&logo=openjdk&logoColor=white" />

- **Framework**: Spring Boot 4.1, Spring Security, Spring Data JPA

  <img src="https://img.shields.io/badge/Spring%20Boot%204.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" />

- **Database**: PostgreSQL, pgvector

  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" />

- **Storage**: AWS S3 (SDK 2.x) / Local File System (조건부 전환)

  <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=flat-square&logo=amazons3&logoColor=white" />

- **External APIs**: Agora RTC/STT, OpenAI, Gemini, DeepL

  <img src="https://img.shields.io/badge/Agora-099DFD?style=flat-square&logo=agora&logoColor=white" />
  <img src="https://img.shields.io/badge/OpenAI-412991?style=flat-square&logo=openai&logoColor=white" />

- **Build / Deploy**: Gradle, Docker, GitHub Actions

  <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white" />

---

## 🏗️ 아키텍처 개요

```text
📱 Mobile App (Flutter)
    │
    ▼
🏥 Centerton Backend (Spring Boot)
    ├─ 🔐 JWT 인증 / 환자 링크 검증
    ├─ 📅 예약 관리 (슬롯 기반)
    ├─ 📹 화상 상담 (Agora RTC + STT)
    ├─ 📋 상담 요약 (Gemini + DeepL 번역)
    ├─ 🩺 사후관리 가이드
    ├─ 🤖 AI 채팅 ──▶ FastAPI RAG Server (내부 네트워크)
    ├─ 📎 파일 업로드 ──▶ AWS S3 / Local Storage
    └─ 🗄️ PostgreSQL (pgvector)
```

---

## 📁 패키지 구조

```text
com.centerton.centerton
├─ domain
│  ├─ patient/                  # 환자 정보, 링크 인증
│  ├─ aftercare/                # 사후관리 케이스, 회복 단계 가이드
│  ├─ appointment/              # 화상 상담 예약, 슬롯 관리
│  ├─ consultation/             # 화상 상담 세션, Agora RTC/STT
│  ├─ consultationsummary/      # 상담 요약, 지시사항
│  ├─ aichat/                   # AI 웰니스 채팅 (RAG 연동)
│  └─ preconsultationsubmission/  # 사전 자료 제출 (파일 업로드)
└─ global
   ├─ config/                   # Security, CORS, UTC 설정
   ├─ jwt/                      # JWT 토큰 발급·검증
   ├─ storage/s3/               # S3 공통 저장소
   ├─ translation/              # DeepL 번역 클라이언트
   ├─ exception/                # 전역 예외 처리
   └─ response/                 # 공통 응답 형식
```

---

## 🔌 주요 API

> Base path: `/api`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/patients/{id}/access-links` | 환자 접근 링크 생성 |
| POST | `/patients/access-links/verify` | 링크 토큰 검증 (JWT 발급) |
| GET | `/appointments` | 예약 목록 조회 |
| POST | `/appointments` | 예약 생성 |
| GET | `/appointments/available-dates` | 가용 예약 날짜 조회 |
| POST | `/consultations/{appointmentId}/join` | 화상 상담 입장 |
| GET | `/aftercare/home` | 사후관리 홈 (N일차, 회복 단계) |
| GET | `/aftercare/emergency-medical-report` | 응급의료 리포트 |
| POST | `/ai-chats` | AI 채팅 메시지 전송 |
| GET | `/ai-chats/rooms` | AI 채팅방 목록 |

---

## ⚙️ 환경변수

| 이름 | 설명 | 기본값 |
|------|------|--------|
| `DATABASE_URL` | PostgreSQL 접속 URL | — |
| `DATABASE_USERNAME` | DB 사용자 | — |
| `DATABASE_PASSWORD` | DB 비밀번호 | — |
| `JWT_SECRET_KEY` | JWT 서명 키 (Base64) | — |
| `JWT_ACCESS_EXPIRATION` | 토큰 만료 시간 (ms) | `3600000` |
| `AGORA_APP_ID` | Agora App ID | — |
| `AGORA_APP_CERTIFICATE` | Agora App Certificate | — |
| `OPENAI_API_KEY` | OpenAI API Key | — |
| `GEMINI_API_KEY` | Gemini API Key | — |
| `DEEPL_AUTH_KEY` | DeepL API Key | — |
| `STORAGE_PROVIDER` | 파일 저장소 (`local` / `s3`) | `local` |
| `AWS_REGION` | S3 리전 (S3 사용 시) | — |
| `AWS_S3_BUCKET` | S3 버킷명 (S3 사용 시) | — |
| `AI_CHAT_RAG_SERVICE_BASE_URL` | RAG FastAPI 서버 주소 | `http://centerton-rag:8001` |

---

## 👥 팀 구성 (Backend)

<table>
  <tr>
    <td align="center" width="180">
      <a href="https://github.com/commata">
        <img src="https://github.com/commata.png" width="120" height="120" style="border-radius:50%" /><br/>
        <b>commata</b>
      </a>
    </td>
    <td align="center" width="180">
      <a href="https://github.com/oroi2009">
        <img src="https://github.com/oroi2009.png" width="120" height="120" style="border-radius:50%" /><br/>
        <b>천성진</b>
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">Backend</td>
    <td align="center">Backend / AI</td>
  </tr>
</table>
