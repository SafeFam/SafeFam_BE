package com.gold.safefam.global.sms;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SolapiSmsSender implements SmsSender {

    private final DefaultMessageService messageService;
    private final String senderNumber;

    public SolapiSmsSender(
            @Value("${solapi.api-key}") String apiKey,
            @Value("${solapi.api-secret}") String apiSecret,
            @Value("${solapi.sender}") String senderNumber
    ) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
        this.senderNumber = senderNumber;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        Message message = new Message();
        message.setFrom(senderNumber);
        message.setTo(phoneNumber);
        message.setText("[SafeFam] 인증번호: " + code);

        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
        } catch (Exception e) {
            throw new SmsDeliveryException("SMS 발송 실패: ", e);
        }
    }
}