package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginIssuesAndStoresTokens() throws Exception {
        User user = saveUser();

        MvcResult result = login("safe@example.com", "safePassword123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String refreshToken = JsonPath.read(result.getResponse().getContentAsString(),
                "$.data.refreshToken");
        assertTrue(refreshTokenRepository.findByUserId(user.getId()).isPresent());
        assertNotEquals(refreshToken,
                refreshTokenRepository.findByUserId(user.getId()).orElseThrow().getTokenHash());
    }

    @Test
    void loginRejectsUnknownEmailAndWrongPassword() throws Exception {
        saveUser();

        login("unknown@example.com", "safePassword123!")
                .andExpect(status().isUnauthorized());
        login("safe@example.com", "wrongPassword")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiRejectsRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void reissueRotatesRefreshTokenAndLogoutInvalidatesIt() throws Exception {
        saveUser();
        MvcResult loginResult = login("safe@example.com", "safePassword123!")
                .andExpect(status().isOk())
                .andReturn();
        String firstRefreshToken = read(loginResult, "$.data.refreshToken");

        MvcResult reissueResult = reissue(firstRefreshToken)
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = read(reissueResult, "$.data.accessToken");
        String rotatedRefreshToken = read(reissueResult, "$.data.refreshToken");
        assertNotEquals(firstRefreshToken, rotatedRefreshToken);

        reissue(firstRefreshToken).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isOk());

        reissue(rotatedRefreshToken).andExpect(status().isUnauthorized());
    }

    private User saveUser() {
        return userRepository.save(new User(
                "safe@example.com",
                passwordEncoder.encode("safePassword123!"),
                "Safe User"
        ));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions reissue(String refreshToken)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String read(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
