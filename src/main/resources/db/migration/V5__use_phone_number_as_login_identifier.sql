-- 전화번호를 회원 식별자와 로그인 ID로 사용함
-- 기존 이메일 데이터는 보존하되 신규 회원가입에서는 더 이상 필수로 받지 않음
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN phone_number SET NOT NULL;
