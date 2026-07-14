package com.gold.safefam.global.sms;

/**
 * SMS 업체와 무관하게 인증번호 발송 기능을 호출하기 위한 인터페이스
 * 실제 업체가 결정되면 이 인터페이스의 구현체만 추가해 인증 서비스와 분리함
 */
public interface SmsSender {

    /** 휴대폰 번호로 인증번호를 발송함. 인증번호는 로그에 남기지 않음 */
    void sendVerificationCode(String phoneNumber, String code);
}
