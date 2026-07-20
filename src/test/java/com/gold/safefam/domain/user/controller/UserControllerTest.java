package com.gold.safefam.domain.user.controller;

import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.security.JwtUtil;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();

        testUser = userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));
        accessToken = "Bearer " + jwtUtil.generateAccessToken(testUser.getId());
    }

    @Test
    void getMeReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("김안전"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-****-5678"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void getMeReturns401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMeChangesName() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "김세이프"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김세이프"));
    }

    @Test
    void withdrawSoftDeletesUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "safefam12"
                                }
                                """))
                .andExpect(status().isNoContent());

        User deleted = userRepository.findById(testUser.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
    }

    @Test
    void withdrawReturns401WhenWrongPassword() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawSkipsPasswordCheckForKakaoUser() throws Exception {
        User kakaoUser = userRepository.save(User.ofKakao("kakao123", "01099999999", "카카오유저"));
        String kakaoToken = "Bearer " + jwtUtil.generateAccessToken(kakaoUser.getId());

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", kakaoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "password": "dummy"
                            }
                            """))
                .andExpect(status().isNoContent());

        User deleted = userRepository.findById(kakaoUser.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
    }

    @Test
    void getSettingsReturnsDefaultValues() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/settings")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoAnalysisEnabled").value(true))
                .andExpect(jsonPath("$.data.pushEnabled").value(true));
    }

    @Test
    void updateSettingsChangesValues() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "autoAnalysisEnabled": false,
                              "pushEnabled": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoAnalysisEnabled").value(false))
                .andExpect(jsonPath("$.data.pushEnabled").value(false));
    }

    @Test
    void updateSettingsChangesOnlyProvidedFields() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "autoAnalysisEnabled": false
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoAnalysisEnabled").value(false))
                .andExpect(jsonPath("$.data.pushEnabled").value(true));
    }

    @Test
    void getSettingsReturns401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSettingsReturns401WhenNoToken() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "autoAnalysisEnabled": false
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }
}