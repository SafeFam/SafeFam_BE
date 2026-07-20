package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.entity.PhoneVerification;
import com.gold.safefam.domain.auth.repository.PhoneVerificationRepository;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.sms.SmsDeliveryException;
import com.gold.safefam.global.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PhoneVerificationFlowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SmsSender smsSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        reset(smsSender);
    }

    @Test
    void verifiedPhoneCanSignUpAndVerificationIsConsumed() throws Exception {
        String code = sendAndCaptureCode("010-1234-5678");

        verifyCode("01012345678", code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("휴대폰 인증이 완료되었습니다."));

        signup("010-1234-5678")
                .andExpect(status().isCreated());

        User user = userRepository.findByPhoneNumber("01012345678").orElseThrow();
        assertEquals("01012345678", user.getPhoneNumber());
        assertFalse(phoneVerificationRepository.findByPhoneNumber("01012345678").isPresent());
    }

    @Test
    void unverifiedPhoneCannotSignUp() throws Exception {
        signup("010-1111-2222")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("휴대폰 인증이 필요합니다."));
    }

    @Test
    void wrongCodeIncrementsFailureCountAndEventuallyBlocksVerification() throws Exception {
        String phoneNumber = "01022223333";
        String issuedCode = sendAndCaptureCode(phoneNumber);
        String wrongCode = issuedCode.equals("000000") ? "999999" : "000000";

        for (int attempt = 1; attempt <= 5; attempt++) {
            verifyCode(phoneNumber, wrongCode)
                    .andExpect(status().isBadRequest());
        }

        assertEquals(5, phoneVerificationRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow()
                .getFailedAttempts());
        verifyCode(phoneNumber, issuedCode)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        String phoneNumber = "01033334444";
        phoneVerificationRepository.save(new PhoneVerification(
                phoneNumber,
                passwordEncoder.encode("123456"),
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(120)
        ));

        verifyCode(phoneNumber, "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("휴대폰 인증번호가 만료되었습니다."));
    }

    @Test
    void resendDuringCooldownIsRejected() throws Exception {
        String phoneNumber = "01044445555";
        sendAndCaptureCode(phoneNumber);

        sendCode(phoneNumber)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("잠시 후 인증번호를 다시 요청해 주세요."));
    }

    @Test
    void smsProviderFailureReturnsServiceUnavailableAndDoesNotStoreCode() throws Exception {
        doThrow(new SmsDeliveryException("failed"))
                .when(smsSender)
                .sendVerificationCode(anyString(), anyString());

        sendCode("01055556666")
                .andExpect(status().isServiceUnavailable());

        assertTrue(phoneVerificationRepository.findByPhoneNumber("01055556666").isEmpty());
    }

    private String sendAndCaptureCode(String phoneNumber) throws Exception {
        sendCode(phoneNumber)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호를 발송했습니다."));

        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsSender).sendVerificationCode(phoneCaptor.capture(), codeCaptor.capture());
        assertNotEquals("", codeCaptor.getValue());
        assertEquals(6, codeCaptor.getValue().length());
        return codeCaptor.getValue();
    }

    private org.springframework.test.web.servlet.ResultActions sendCode(String phoneNumber)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/phone-verifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phoneNumber + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions verifyCode(
            String phoneNumber,
            String code
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/phone-verifications/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phoneNumber
                        + "\",\"code\":\"" + code + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions signup(String phoneNumber)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phoneNumber
                        + "\",\"password\":\"safefam12\",\"name\":\"Safe User\"}"));
    }
}
