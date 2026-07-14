package com.gold.safefam.global.config;

import com.gold.safefam.global.sms.SmsDeliveryException;
import com.gold.safefam.global.sms.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SMS 업체 구현체가 아직 등록되지 않은 환경에서도 애플리케이션이 시작되게 함
 * 발송 요청은 명시적으로 실패시켜 실제 전송 없이 성공한 것처럼 응답하는 일을 막음
 */
@Configuration
public class SmsConfig {

    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender unavailableSmsSender() {
        // SENS, Solapi 등의 실제 SmsSender Bean이 등록되면 이 기본 Bean은 생성되지 않음
        return (phoneNumber, code) -> {
            throw new SmsDeliveryException("SMS provider is not configured");
        };
    }
}
