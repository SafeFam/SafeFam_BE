package com.gold.safefam.domain.analysis.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller부터 분석 엔진과 JPA 저장까지 생성 API의 전체 세로 흐름을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisCreationFlowTest {

    private static final String DANGEROUS_CONTENT =
            "[긴급] 국민은행 계좌가 정지됩니다. 010-1234-5678로 연락하거나 "
                    + "https://bit.ly/secure-login 에서 인증번호를 입력하고 안전계좌로 이체하세요.";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String accessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(new User(
                "01011112222",
                "encoded-password",
                "분석사용자"
        ));
        accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
    }

    @Test
    void authenticatedRequestAnalyzesAndStoresProtectedResult() throws Exception {
        mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("sms-001", DANGEROUS_CONTENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.analysisId").isNumber())
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.category").value("FINANCIAL_INSTITUTION"))
                .andExpect(jsonPath("$.data.scoreBreakdown.llmScore").value(0))
                .andExpect(jsonPath("$.data.urls[0].suspicious").value(true));

        transactionTemplate.executeWithoutResult(status -> {
            assertEquals(1, analysisRepository.count());
            Analysis stored = analysisRepository.findAll().get(0);
            assertEquals(64, stored.getContentHash().length());
            assertNotEquals(DANGEROUS_CONTENT, stored.getContentHash());
            assertFalse(stored.getContentPreview().contains("010-1234-5678"));
            assertFalse(stored.getContentPreview().contains("https://bit.ly"));
            assertTrue(stored.getContentPreview().contains("010-****-****"));
            assertTrue(stored.getContentPreview().contains("[URL]"));
            assertFalse(stored.getIndicators().isEmpty());
            assertFalse(stored.getUrlRisks().isEmpty());
        });
    }

    @Test
    void sameClientMessageIdReturnsExistingAnalysisWithoutDuplicateRow() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("sms-duplicate", DANGEROUS_CONTENT)))
                .andExpect(status().isOk())
                .andReturn();

        String firstId = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(),
                "$.data.analysisId"
        ).toString();

        MvcResult second = mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("sms-duplicate", "완전히 다른 두 번째 문자")))
                .andExpect(status().isOk())
                .andReturn();
        String secondId = com.jayway.jsonpath.JsonPath.read(
                second.getResponse().getContentAsString(),
                "$.data.analysisId"
        ).toString();

        assertEquals(firstId, secondId);
        assertEquals(1, analysisRepository.count());
    }

    @Test
    void requestWithoutAccessTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("sms-unauthorized", DANGEROUS_CONTENT)))
                .andExpect(status().isUnauthorized());
    }

    private String requestBody(String clientMessageId, String content) {
        return """
                {
                  "clientMessageId": "%s",
                  "sender": "국민은행",
                  "content": "%s",
                  "receivedAt": "2026-07-17T15:00:00+09:00",
                  "source": "AUTO"
                }
                """.formatted(clientMessageId, content);
    }
}
