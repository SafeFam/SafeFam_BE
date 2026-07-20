package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.entity.PhoneVerification;
import com.gold.safefam.domain.auth.repository.PhoneVerificationRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.kakao.KakaoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        phoneVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesUserWithEncodedPassword() throws Exception {
        PhoneVerification verification = new PhoneVerification(
                "01012345678",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-1234-5678",
                                  "password": "safefam12",
                                  "name": "김안전"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        User user = userRepository.findByPhoneNumber("01012345678").orElseThrow();

        assertNotEquals("safefam12", user.getPassword());
        assertTrue(passwordEncoder.matches("safefam12", user.getPassword()));
        assertTrue(user.getPhoneNumber().equals("01012345678"));
    }

    @Test
    void signupReturnsConflictWhenPhoneNumberAlreadyExists() throws Exception {
        userRepository.save(new User(
                "01099999999",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));
        PhoneVerification verification = new PhoneVerification(
                "01099999999",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-9999-9999",
                                  "password": "safefam12",
                                  "name": "김안전"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("이미 가입된 휴대폰 번호입니다."));
    }

    @Test
    void resetPasswordChangesPassword() throws Exception {
        // 기존 사용자 저장
        userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        // 전화번호 인증 완료 상태 만들기
        PhoneVerification verification = new PhoneVerification(
                "01012345678",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        // 비밀번호 재설정
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678",
                              "newPassword": "newpass12"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        // 새 비밀번호로 로그인 확인
        User user = userRepository.findByPhoneNumber("01012345678").orElseThrow();
        assertTrue(passwordEncoder.matches("newpass12", user.getPassword()));
    }

    @Test
    void resetPasswordFailsWhenPhoneNotVerified() throws Exception {
        userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678",
                              "newPassword": "newpass12"
                            }
                            """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("휴대폰 인증이 필요합니다."));
    }

    @Test
    void resetPasswordFailsWhenUserNotFound() throws Exception {
        PhoneVerification verification = new PhoneVerification(
                "01099999999",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-9999-9999",
                              "newPassword": "newpass12"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("전화번호 또는 비밀번호가 올바르지 않습니다."));
    }

    @MockitoBean
    private KakaoClient kakaoClient;

    @Test
    void kakaoLoginReturnsNewUserWhenNotRegistered() throws Exception {
        when(kakaoClient.getUserInfo(anyString()))
                .thenReturn(Map.of("id", 12345678L));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "kakaoAccessToken": "test-token"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.kakaoId").value("12345678"));
    }

    @Test
    void kakaoSignupCreatesUserAndReturnsToken() throws Exception {
        when(kakaoClient.getUserInfo(anyString()))
                .thenReturn(Map.of("id", 12345678L));

        PhoneVerification verification = new PhoneVerification(
                "01012345678",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/kakao/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "kakaoAccessToken": "test-token",
                              "phoneNumber": "010-1234-5678",
                              "name": "김안전"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void loginLocksAccountAfterFiveFailures() throws Exception {
        userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "phoneNumber": "010-1234-5678",
                                  "password": "wrongpassword"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678",
                              "password": "wrongpassword"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("계정이 잠겼습니다. 휴대폰 인증으로 잠금을 해제해 주세요."));
    }

    @Test
    void unlockResetsAccountLock() throws Exception {
        User user = userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "phoneNumber": "010-1234-5678",
                                  "password": "wrongpassword"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        PhoneVerification verification = new PhoneVerification(
                "01012345678",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("계정 잠금이 해제되었습니다."));

        User unlocked = userRepository.findByPhoneNumber("01012345678").orElseThrow();
        assertFalse(unlocked.isLocked());
        assertEquals(0, unlocked.getLoginFailCount());
    }

    @Test
    void successfulLoginResetsFailCount() throws Exception {
        userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "phoneNumber": "010-1234-5678",
                                  "password": "wrongpassword"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678",
                              "password": "safefam12"
                            }
                            """))
                .andExpect(status().isOk());

        User updated = userRepository.findByPhoneNumber("01012345678").orElseThrow();
        assertEquals(0, updated.getLoginFailCount());
        assertFalse(updated.isLocked());
    }

    @Test
    void passwordResetUnlocksAccount() throws Exception {
        userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "phoneNumber": "010-1234-5678",
                                  "password": "wrongpassword"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        PhoneVerification verification = new PhoneVerification(
                "01012345678",
                passwordEncoder.encode("123456"),
                Instant.now().plusSeconds(180),
                Instant.now()
        );
        verification.markVerified(Instant.now());
        phoneVerificationRepository.save(verification);

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "phoneNumber": "010-1234-5678",
                              "newPassword": "newpass12"
                            }
                            """))
                .andExpect(status().isOk());

        User updated = userRepository.findByPhoneNumber("01012345678").orElseThrow();
        assertFalse(updated.isLocked());
        assertEquals(0, updated.getLoginFailCount());
    }
}
