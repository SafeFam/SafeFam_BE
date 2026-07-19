package com.gold.safefam.global.sms;

/** SMS 업체 호출 실패를 인증 도메인에 전달하기 위한 인프라 예외 */
public class SmsDeliveryException extends RuntimeException {

    public SmsDeliveryException(String message) {
        super(message);
    }

    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}