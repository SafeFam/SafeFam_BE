package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.entity.PhoneVerification;
import com.gold.safefam.domain.auth.repository.PhoneVerificationRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                                  "email": "safe@example.com",
                                  "password": "safePassword123!",
                                  "name": "김안전"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        User user = userRepository.findByEmail("safe@example.com").orElseThrow();

        assertNotEquals("safePassword123!", user.getPassword());
        assertTrue(passwordEncoder.matches("safePassword123!", user.getPassword()));
        assertTrue(user.getPhoneNumber().equals("01012345678"));
    }

    @Test
    void signupReturnsConflictWhenEmailAlreadyExists() throws Exception {
        userRepository.save(new User(
                "duplicate@example.com",
                passwordEncoder.encode("safePassword123!"),
                "김안전"
        ));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-9999-9999",
                                  "email": "duplicate@example.com",
                                  "password": "safePassword123!",
                                  "name": "김안전"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }
}
