# SafeFam Backend

> AI 기반 스미싱 탐지 서비스

## 📁 패키지 구조

```
com.gold.safefam
├── domain
│   ├── analysis      # 링크·메시지 위험도 분석 및 AI 결과 처리
│   ├── auth          # 인증·인가 (JWT, 카카오 OAuth, SMS 휴대폰 인증)
│   ├── chat          # 분석 결과 기반 멀티턴 챗봇과 FastAPI 프록시
│   ├── family        # 가족 그룹 생성·초대·관리
│   ├── notification  # FCM 푸시 알림 및 디바이스 토큰 관리
│   ├── report        # 피싱 링크 신고
│   ├── statistics    # 분석 이력 통계 집계
│   ├── user          # 사용자 조회·수정·탈퇴
│   └── whitelist     # 안전 URL 화이트리스트 관리
├── global            # 공통 설정, 보안 필터, 예외 처리, 외부 연동 (Kakao, SMS)
└── infrastructure    # 메시징 인프라 (RabbitMQ, Transactional Outbox, 중복 처리)
```

## ⚙️ 실행 방법

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 아래 항목을 반드시 수정하세요:

| 변수 | 설명 |
|------|------|
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `DB_URL` | Spring에서 직접 사용하는 DB JDBC URL (Docker 외부 실행 시) |
| `DB_USERNAME` / `DB_PASSWORD` | DB 접속 계정 (Docker 외부 실행 시) |
| `RABBITMQ_PASSWORD` | RabbitMQ 비밀번호 |
| `RABBITMQ_HOST` | RabbitMQ 호스트 (Docker 외부 실행 시 `localhost`) |
| `JWT_SECRET` | JWT 서명 키 (Base64 인코딩) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access 토큰 만료 시간 (ms, 기본 30분) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh 토큰 만료 시간 (ms, 기본 7일) |
| `KAKAO_CLIENT_ID` | 카카오 OAuth 클라이언트 ID |
| `OUTBOX_ENCRYPTION_KEY_BASE64` | Outbox 페이로드 암호화 키 (32바이트, Base64) |
| `SOLAPI_API_KEY` / `SOLAPI_API_SECRET` | Solapi SMS 인증 키 |
| `SOLAPI_SENDER` | SMS 발신 번호 |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Firebase Admin SDK 서비스 계정 JSON 파일 경로 |
| `AI_CHAT_BASE_URL` | Spring이 호출할 FastAPI 주소 (Compose 기본값 `http://fastapi-ai:8000`) |
| `AI_CHAT_CONNECT_TIMEOUT` / `AI_CHAT_READ_TIMEOUT` | FastAPI 연결·응답 제한 시간 |
| `SENDER_CANDIDATE_REVIEW_THRESHOLD` | 기관 발신번호 후보를 사람 검토 대상으로 전환할 관찰 횟수 (기본 3회) |

### 2. Docker Compose로 전체 실행

```bash
docker compose up --build
```

> AI 서버(`SafeFam_AI`)는 `../SafeFam_AI` 경로에 위치해야 합니다.

## 📑 API 문서

서버 실행 후 아래 주소에서 Swagger UI를 확인할 수 있습니다:

```
# spring-backend를 직접 실행한 경우
http://localhost:8080/swagger-ui/index.html

# nginx까지 실행한 경우
http://localhost/api/swagger-ui/index.html
```

`POST /api/v1/chat`의 실제 AI 응답까지 확인하려면 `fastapi-ai`도 함께 실행해야 합니다.
