-- 회원가입 전 휴대폰 인증 요청의 최신 상태를 번호별로 하나씩 보관함
-- code_hash에는 인증번호 원문이 아닌 BCrypt 해시를 저장함
CREATE TABLE phone_verifications (
    id               BIGSERIAL PRIMARY KEY,
    phone_number     VARCHAR(20) NOT NULL UNIQUE,
    code_hash        VARCHAR(60) NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_sent_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    failed_attempts  INT         NOT NULL DEFAULT 0,
    verified_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_phone_verifications_expires_at
    ON phone_verifications(expires_at);
