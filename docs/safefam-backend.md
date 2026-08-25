# SafeFam 백엔드 개발 컨벤션

> SafeFam 백엔드 개발 시 공통으로 지켜야 하는 패키지 구조, 네이밍, API, DB, Git 규칙을 정의합니다.
>
> 기준 기술 스택: Java 17, Spring Boot 4.1, Spring Data JPA, PostgreSQL, JWT

---

## 1. 패키지 / 폴더 구조 — 도메인형

기능(도메인) 단위로 패키지를 분리합니다. 공통 인프라성 코드는 `global` 하위에 둡니다.

```text
com.gold.safefam
├─ global
│  ├─ config          # 스프링 설정, Bean 등록, Swagger 설정
│  ├─ entity          # 공통 JPA Entity 기반 클래스(BaseTimeEntity 등)
│  ├─ security        # 인증/인가, JWT, Spring Security 설정
│  ├─ exception       # 공통 예외, 전역 예외 핸들러, ErrorCode
│  └─ response        # 공통 응답 포맷(ApiResponse, PageResponse 등)
└─ domain
   ├─ auth            # 회원가입, 로그인, 토큰 발급·재발급
   ├─ user            # 사용자 정보, 탐지·알림 설정
   ├─ analysis        # 금융 사기 문자 분석, 탐지 이력, 피드백
   ├─ notification    # 푸시 알림 기기 관리
   └─ statistics      # 탐지 통계, 대시보드
```

### 도메인 패키지 내부 구조

각 도메인 패키지는 역할별로 하위 패키지를 둡니다.

```text
analysis
├─ controller
├─ service
├─ repository
├─ entity
├─ dto
└─ enums
```

- 한 도메인에서만 사용하는 코드는 해당 도메인 패키지 안에 둡니다.
- 두 개 이상의 도메인이 공유하는 인프라성 코드만 `global`로 올립니다.
- 여러 Entity가 공유하는 생성·수정·삭제 시간 필드는 `global.entity.BaseTimeEntity`에서 관리합니다.
- JPA Entity는 해당 도메인의 `entity` 패키지에 둡니다.
- 도메인별 Enum은 해당 도메인의 `enums` 패키지에 둡니다.
- DTO는 현재처럼 `dto` 패키지에 두고 `Request`, `Response` 접미사로 역할을 구분합니다.
- `service`, `repository`, `entity` 등 아직 없는 패키지는 실제 구현이 추가될 때 생성합니다.
- 서로 다른 도메인을 직접 강하게 결합하지 않습니다. 필요한 경우 서비스 계층을 통해 협력합니다.

---

## 2. 코드 네이밍 컨벤션

| 항목 | 규칙 | 예시 |
| --- | --- | --- |
| class / interface / record | UpperCamelCase | `AnalysisService`, `UserRepository` |
| package | 소문자 | `analysis`, `repository` |
| method | lowerCamelCase | `analyzeMessage`, `getUserSettings` |
| 변수 | lowerCamelCase | `riskScore`, `analysisCount` |
| Enum 타입명 | UpperCamelCase | `RiskLevel` |
| Enum 상수 | UPPER_SNAKE_CASE | `FALSE_POSITIVE`, `LAST_30_DAYS` |
| 상수(`static final`) | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |

### 클래스 역할별 접미사

계층과 역할이 이름에서 드러나도록 접미사를 통일합니다.

| 역할 | 접미사 | 예시 |
| --- | --- | --- |
| 컨트롤러 | `Controller` | `AnalysisController` |
| 서비스 | `Service` | `AnalysisService` |
| 리포지토리 | `Repository` | `AnalysisRepository` |
| 요청 DTO | `Request` | `AnalysisRequest` |
| 응답 DTO | `Response` | `AnalysisResponse` |
| 예외 | `Exception` | `AnalysisNotFoundException` |
| 설정 클래스 | `Config` | `SecurityConfig` |

### DTO 작성 규칙

- 단순 데이터 전달용 DTO는 Java `record`를 우선 사용합니다.
- Request DTO에는 Bean Validation을 사용해 입력값을 검증합니다.
- API 문서에 필요한 설명과 예시는 `@Schema`로 작성합니다.
- Entity를 API 응답으로 직접 반환하지 않습니다.

```java
public record AnalysisRequest(
        @Size(max = 100)
        String sender,

        @NotBlank
        @Size(max = 5000)
        String content,

        @NotNull
        OffsetDateTime receivedAt,

        @NotNull
        AnalysisSource source
) {
}
```

---

## 3. 파일명 컨벤션

- 클래스명과 파일명은 동일하게 작성합니다.
  - `AnalysisService` → `AnalysisService.java`
  - `RiskLevel` → `RiskLevel.java`
- 설정 파일은 역할과 환경을 이름에서 구분할 수 있게 작성합니다.
  - `application.yml`
  - `application-test.yml`
- 하나의 Java 파일에는 원칙적으로 하나의 최상위 타입만 선언합니다.
- 응답에 종속되는 작은 구조는 해당 응답 DTO 내부의 중첩 `record`로 선언할 수 있습니다.

---

## 4. DB 컨벤션

- 테이블명과 컬럼명은 모두 `snake_case`를 사용합니다.
  - `analysis_history`
  - `created_at`
  - `risk_score`
  - `user_id`
- Java Entity 필드는 `lowerCamelCase`를 사용합니다.
- 이름이 자동 매핑되지 않거나 의미를 명확히 해야 하는 경우 `@Column(name = "...")`을 지정합니다.
- Enum 컬럼은 반드시 문자열로 저장합니다.
- 사용자 인증 정보는 이메일 기반 일반 회원가입을 우선 지원하며, 비밀번호는 `password_hash` 컬럼에 단방향 해시로 저장합니다.
- 삭제 시점을 기록해야 하는 Entity는 `deleted_at` 컬럼을 사용합니다.

```java
@Enumerated(EnumType.STRING)
@Column(name = "risk_level", nullable = false)
private RiskLevel riskLevel;
```

- 외래 키 컬럼은 `{참조_테이블_단수형}_id` 형식을 사용합니다.
- 날짜·시간은 서버 내부에서 `OffsetDateTime` 사용을 우선하며, API에서는 ISO-8601 형식으로 전달합니다.
- 운영 환경에서는 스키마 변경을 마이그레이션 도구로 관리합니다. `ddl-auto: update`에 의존하지 않습니다.
- 기본 애플리케이션 설정은 `ddl-auto: validate`를 사용하며, 테스트 프로필은 H2 인메모리 DB를 사용합니다.
- 문자 원문처럼 민감할 수 있는 데이터는 저장 여부와 보관 기간을 명확히 하고, 로그에 그대로 남기지 않습니다.

---

## 5. API 공통 규칙

### 5.1 Base URL과 버전

SafeFam API의 기본 경로는 다음과 같습니다.

```text
/api/v1
```

주요 도메인별 경로는 다음 규칙을 따릅니다.

| 도메인 | 경로 |
| --- | --- |
| 인증 | `/api/v1/auth` |
| 사용자 | `/api/v1/users/me` |
| 문자 분석·탐지 이력 | `/api/v1/analyses` |
| 분석 결과 기반 챗봇 | `/api/v1/chat` |
| 통계 | `/api/v1/statistics` |
| 푸시 알림 기기 | `/api/v1/devices` |

- URL에는 동사보다 리소스 명사를 사용합니다.
- 리소스명은 복수형을 사용합니다.
- 하위 리소스와 행위는 의미가 명확한 경로로 표현합니다.
  - `POST /api/v1/analyses/{analysisId}/feedback`
  - `GET /api/v1/statistics/overview`

### 5.2 인증 방식

- 인증 방식: `Bearer JWT`
- 인증 헤더:

```http
Authorization: Bearer {accessToken}
```

- 별도 표기가 없는 API는 로그인 사용자만 호출할 수 있습니다.
- 다음 인증 API만 Access Token 없이 호출할 수 있습니다.
  - `POST /api/v1/auth/signup`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/reissue`
  - `POST /api/v1/auth/phone-verifications/**`
  - `POST /api/v1/auth/kakao`
  - `POST /api/v1/auth/kakao/signup`
  - `POST /api/v1/auth/password/reset`
  - `POST /api/v1/auth/unlock`
- 로그아웃 API는 인증이 필요합니다.

### 5.3 Content-Type

```http
Content-Type: application/json
```

### 5.4 공통 응답 형식

모든 JSON API 응답은 `global.response.ApiResponse<T>`로 감싸 일관된 형태로 반환합니다.

#### 성공 응답

```json
{
  "status": "SUCCESS",
  "message": "문자 분석이 완료되었습니다.",
  "data": {
    "analysisId": 101,
    "riskScore": 92,
    "riskLevel": "HIGH",
    "category": "FINANCIAL_INSTITUTION"
  }
}
```

#### 실패 응답

```json
{
  "status": "ERROR",
  "message": "분석 이력을 찾을 수 없습니다.",
  "data": null
}
```

- `status`는 성공 시 `SUCCESS`, 실패 시 `ERROR`를 사용합니다.
- `message`는 사용자가 이해할 수 있는 간결한 한국어 문장으로 작성합니다.
- `data`는 실제 응답 데이터이며, 반환할 데이터가 없으면 `null`을 사용합니다.
- HTTP 상태 코드를 함께 올바르게 사용합니다. 응답 본문의 `status`만으로 성공·실패를 표현하지 않습니다.
- 민감한 내부 정보, 예외 메시지, SQL, 스택 트레이스는 응답에 포함하지 않습니다.

### 5.5 페이지네이션 형식

목록 데이터가 계속 증가할 수 있는 API는 페이지네이션을 사용합니다.

#### 요청 파라미터

| 파라미터 | 설명 | 기본값 |
| --- | --- | --- |
| `page` | 0부터 시작하는 페이지 번호 | `0` |
| `size` | 페이지 크기 | `20` |

- `page`는 0 이상이어야 합니다.
- `size`는 1 이상 100 이하여야 합니다.
- 탐지 이력은 기본적으로 최신 분석 순으로 조회합니다.
- 도메인 필터는 쿼리 파라미터로 전달합니다.

```http
GET /api/v1/analyses?page=0&size=20&riskLevel=HIGH&category=LOAN&from=2026-06-01&to=2026-06-30
```

#### 응답 예시

```json
{
  "status": "SUCCESS",
  "message": "탐지 이력을 조회했습니다.",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  }
}
```

### 5.6 공통 HTTP 상태 코드

| 코드 | 사용 상황 |
| --- | --- |
| `200 OK` | 조회, 수정, 분석 완료 |
| `201 Created` | 회원, 기기 등 리소스 생성 |
| `204 No Content` | 삭제 또는 해제 성공이며 응답 본문이 필요 없는 경우 |
| `400 Bad Request` | 형식 오류, 필수값 누락, 유효성 검증 실패 |
| `401 Unauthorized` | 인증 정보 없음, Access Token 만료 또는 위조 |
| `403 Forbidden` | 인증됐지만 해당 리소스에 접근할 권한이 없음 |
| `404 Not Found` | 사용자, 분석 이력, 기기 등 대상 리소스가 없음 |
| `409 Conflict` | 이메일 중복, 중복 요청, 현재 상태와 요청 충돌 |
| `422 Unprocessable Entity` | 요청 형식은 유효하지만 비즈니스 규칙 위반 |
| `500 Internal Server Error` | 처리되지 않은 서버 내부 오류 |
| `502 Bad Gateway` | 외부 AI·URL 검사 서비스의 잘못된 응답 |
| `503 Service Unavailable` | 외부 분석 서비스 일시 장애 또는 서버 과부하 |

### 5.7 공통 Enum

Enum 타입명은 `UpperCamelCase`, 상수는 `UPPER_SNAKE_CASE`를 사용하며 DB에는 문자열로 저장합니다.

| Enum | 값 |
| --- | --- |
| `AnalysisSource` | `AUTO`, `MANUAL` |
| `RiskLevel` | `LOW`, `MEDIUM`, `HIGH` |
| `PhishingCategory` | `FINANCIAL_INSTITUTION`, `GOVERNMENT_AGENCY`, `LOAN`, `JOB`, `DELIVERY`, `MESSENGER`, `OTHER` |
| `IndicatorType` | `IMPERSONATION`, `FINANCIAL_ACTION`, `SENSITIVE_INFORMATION`, `URGENCY`, `SHORTENED_URL`, `MALICIOUS_URL` |
| `FeedbackType` | `CORRECT`, `FALSE_POSITIVE`, `FALSE_NEGATIVE` |
| `DevicePlatform` | `ANDROID`, `IOS` |
| `StatisticsPeriod` | `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_90_DAYS`, `ALL` |

Enum을 추가하거나 변경하면 다음 항목을 함께 확인합니다.

1. OpenAPI/Swagger 문서
2. DB 저장값과 마이그레이션
3. 프론트엔드 타입과 표시 문구
4. 기존 데이터와의 하위 호환성

### 5.8 `ApiResponse` / `PageResponse`

`ApiResponse`는 SafeFam의 공통 응답 구조로 사용합니다.

```java
package com.gold.safefam.global.response;

public record ApiResponse<T>(
        String status,
        String message,
        T data
) {
}
```

페이지 목록은 `PageResponse<T>`를 `ApiResponse`의 `data`로 전달합니다.

```java
package com.gold.safefam.global.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
```

- 응답 객체를 컨트롤러마다 임의로 새로 정의하지 않습니다.
- 공통 응답 생성이 반복되면 `success`, `error` 정적 팩터리 메서드를 추가해 사용합니다.
- `204 No Content` 응답에는 `ApiResponse` 본문을 넣지 않습니다.

### 5.9 예외 처리

- 비즈니스 예외는 공통 `BusinessException`을 상속하거나 `ErrorCode`를 포함하도록 통일합니다.
- `@RestControllerAdvice` 기반의 `GlobalExceptionHandler`에서 예외를 한곳에 모아 처리합니다.
- HTTP 상태 코드는 각 `ErrorCode`가 가진 상태를 기준으로 반환합니다.
- Validation 오류도 공통 응답 형식으로 변환합니다.
- 컨트롤러에서 반복적인 `try-catch`로 예외 응답을 직접 만들지 않습니다.

ErrorCode 접두사는 다음과 같이 구분합니다.

| 범위 | 접두사 | 예시 |
| --- | --- | --- |
| 공통 | `C` | `C001` |
| 인증 | `AU` | `AU001` |
| 사용자 | `US` | `US001` |
| 문자 분석 | `AN` | `AN001` |
| 알림 | `NO` | `NO001` |
| 통계 | `ST` | `ST001` |

예시:

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "C999",
            "서버 오류가 발생했습니다."
    ),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AU001", "인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US001", "사용자를 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "AN001", "분석 이력을 찾을 수 없습니다."),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NO001", "등록된 기기를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

> 현재 `ApiResponse`는 `status`, `message`, `data` 구조입니다. `ErrorCode.code`를 클라이언트에 전달하려면 팀 합의 후 응답 스키마에 `code` 필드를 추가하고, 백엔드·프론트엔드·API 문서를 한 번에 변경합니다.

### 5.10 Swagger / OpenAPI

- 모든 컨트롤러에는 `@Tag`를 작성합니다.
- 모든 엔드포인트에는 `@Operation`으로 요약과 필요한 설명을 작성합니다.
- 인증이 필요한 API에는 `bearerAuth` 보안 요구사항을 적용합니다.
- DTO에는 필요한 `@Schema` 설명과 예시를 작성합니다.
- 로컬 Swagger UI 경로:

```text
/swagger-ui.html
```

브라우저에서 리다이렉트된 실제 UI 경로는 다음과 같이 보일 수 있습니다.

```text
/swagger-ui/index.html
```

- OpenAPI JSON 경로:

```text
/v3/api-docs
```

---

## 6. 보안·개인정보 처리 규칙

SafeFam은 금융 사기 의심 문자와 사용자 정보를 처리하므로 다음 규칙을 반드시 지킵니다.

- 비밀번호는 단방향 해시로 저장하며 원문을 저장하거나 로그에 남기지 않습니다.
- JWT, Refresh Token, 기기 토큰, DB 비밀번호를 코드에 하드코딩하지 않습니다.
- 민감한 설정은 환경 변수 또는 별도 시크릿 저장소로 관리합니다.
- 문자 원문 저장 기본값은 비활성화합니다.
- 문자 발신자, URL, 계좌번호, 전화번호 등 민감할 수 있는 값은 로그에서 마스킹합니다.
- 분석 이력 조회·수정·삭제 시 반드시 현재 사용자 소유의 데이터인지 확인합니다.
- 외부 AI 또는 URL 검사 서비스로 데이터를 전송할 때 최소한의 정보만 전달합니다.
- 예제와 테스트 데이터에는 실제 개인정보를 사용하지 않습니다.

---

## 7. Git 컨벤션

### 7.1 브랜치 전략

- `develop`은 기본 브랜치이자 통합 브랜치입니다.
- `main`은 최종 배포 또는 안정화 브랜치로 사용합니다.
- `develop`과 `main`에 직접 push하지 않습니다.
- 작업 브랜치는 최신 `develop`에서 분기하고 PR을 통해 병합합니다.
- 하나의 브랜치는 하나의 이슈 또는 하나의 명확한 작업만 다룹니다.
- PR 병합 전 불필요한 작업용 브랜치를 삭제하지 않습니다.
- 병합이 완료된 브랜치는 삭제합니다.

```bash
git switch develop
git pull origin develop
git switch -c feat/12-sms-analysis
```

### 7.2 작업 순서

1. 이슈를 생성하고 작업 범위, 완료 조건, 담당자, 라벨을 작성합니다.
2. 로컬 `develop`을 최신 상태로 갱신합니다.
3. `{type}/{이슈번호}-{기능명}` 형식으로 브랜치를 생성합니다.
4. 기능을 구현하고 테스트한 뒤 의미 있는 단위로 커밋합니다.
5. 원격 브랜치에 push하고 PR을 생성합니다.
6. PR 본문에 작업 내용, 테스트 결과, 관련 이슈를 작성합니다.
7. 최소 2명의 승인을 받은 뒤 병합합니다.
8. 병합 후 원격·로컬 작업 브랜치를 정리합니다.

### 7.3 브랜치 / 커밋 / PR 네이밍

| 항목 | 규칙 | 예시 |
| --- | --- | --- |
| 브랜치 | `{type}/{이슈번호}-{기능명}` | `feat/12-sms-analysis` |
| 커밋 메시지 | `{type}: 설명 (#{이슈번호})` | `feat: 문자 분석 API 추가 (#12)` |
| PR 제목 | `{Type}(#{이슈번호}): 핵심 내용` | `Feat(#12): 문자 분석 API 구현` |

사용 가능한 type:

| type | 용도 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `style` | 포맷, 공백 등 코드 동작에 영향 없는 수정 |
| `test` | 테스트 추가·수정 |
| `chore` | 설정, 빌드, 의존성, 기타 작업 |
| `docs` | 문서 추가·수정 |

### 7.4 커밋 규칙

- 커밋 제목은 무엇을 변경했는지 한눈에 알 수 있게 작성합니다.
- 서로 다른 목적의 변경을 하나의 커밋에 섞지 않습니다.
- 빌드 산출물, IDE 개인 설정, 시크릿 파일은 커밋하지 않습니다.
- 작업 중 사용한 임시 로그와 디버깅 코드는 제거합니다.
- 커밋 전 최소한 다음을 확인합니다.

```bash
./gradlew test
```

Windows에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat test
```

### 7.5 PR 체크리스트

- [ ] 관련 이슈를 연결했습니다.
- [ ] 구현 범위와 변경 이유를 설명했습니다.
- [ ] 로컬 테스트를 통과했습니다.
- [ ] API 변경 사항을 Swagger/OpenAPI 문서에 반영했습니다.
- [ ] DB 변경 사항과 마이그레이션을 포함했습니다.
- [ ] 민감 정보가 코드·로그·테스트 데이터에 포함되지 않았습니다.
- [ ] 프론트엔드에 영향을 주는 응답 스키마 또는 Enum 변경을 공유했습니다.
- [ ] 병합 전 작업 브랜치를 삭제하지 않았습니다.
