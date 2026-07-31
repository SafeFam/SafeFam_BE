## PII 마스킹 토큰 형식

Spring `PiiMaskingService`에서 RabbitMQ 페이로드 생성 직전에 적용 <br>
FastAPI와 토큰 형식을 공유하며, 변경 시 양쪽 레포를 동시에 수정해야 함

| 토큰 | 의미 |
|---|---|
| `[RRN]` | 주민등록번호 |
| `[CARD]` | 카드번호 |
| `[ACCOUNT]` | 계좌번호 |
| `[PHONE]` | 전화번호 |
| `[EMAIL]` | 이메일 주소 |

**변경 절차**
1. `SafeFam_BE`와 `SafeFam_AI` 양쪽 레포에서 토큰 형식 동시 수정
2. 양쪽 테스트 통과 확인
3. Spring → FastAPI 순서로 배포
