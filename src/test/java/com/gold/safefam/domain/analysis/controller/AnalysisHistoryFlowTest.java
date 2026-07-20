package com.gold.safefam.domain.analysis.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisFeedback;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.FeedbackType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisFeedbackRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 이력 목록·상세·삭제·피드백 API의 인증 사용자 경계를 통합 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisHistoryFlowTest {

    private static final String HASH = "a".repeat(64);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private AnalysisFeedbackRepository analysisFeedbackRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private User owner;
    private User otherUser;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        analysisFeedbackRepository.deleteAll();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(new User("01011112222", "encoded-password", "이력사용자"));
        otherUser = userRepository.save(new User("01033334444", "encoded-password", "다른사용자"));
        ownerToken = "Bearer " + jwtUtil.generateAccessToken(owner.getId(), owner.getRole());
    }

    @Test
    void listReturnsOnlyOwnedAnalysesInLatestOrderAndSupportsFilters() throws Exception {
        Analysis oldest = saveAnalysis(
                owner.getId(),
                "01011112222",
                "오래된 정상 문자",
                RiskLevel.LOW,
                PhishingCategory.OTHER,
                10,
                "2026-07-01T09:00:00+09:00"
        );
        Analysis highRisk = saveAnalysis(
                owner.getId(),
                "15881234",
                "금융기관 사칭 의심 문자",
                RiskLevel.HIGH,
                PhishingCategory.FINANCIAL_INSTITUTION,
                92,
                "2026-07-10T09:00:00+09:00"
        );
        Analysis latest = saveAnalysis(
                owner.getId(),
                "01099998888",
                "최근 배송 문자",
                RiskLevel.MEDIUM,
                PhishingCategory.DELIVERY,
                55,
                "2026-07-15T09:00:00+09:00"
        );
        saveAnalysis(
                otherUser.getId(),
                "01033334444",
                "다른 사용자 문자",
                RiskLevel.HIGH,
                PhishingCategory.FINANCIAL_INSTITUTION,
                99,
                "2026-07-20T09:00:00+09:00"
        );

        mockMvc.perform(get("/api/v1/analyses")
                        .header("Authorization", ownerToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content[0].analysisId").value(latest.getId()))
                .andExpect(jsonPath("$.data.content[0].maskedSender").value("010-****-8888"))
                .andExpect(jsonPath("$.data.content[1].analysisId").value(highRisk.getId()))
                .andExpect(jsonPath("$.data.content[1].maskedSender").value("1588****"));

        mockMvc.perform(get("/api/v1/analyses")
                        .header("Authorization", ownerToken)
                        .param("riskLevel", "HIGH")
                        .param("category", "FINANCIAL_INSTITUTION")
                        .param("from", "2026-07-10")
                        .param("to", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].analysisId").value(highRisk.getId()))
                .andExpect(jsonPath("$.data.content[0].messagePreview").value("금융기관 사칭 의심 문자"));

        assertTrue(analysisRepository.existsById(oldest.getId()));
    }

    @Test
    void detailReturnsOwnedAnalysisAndHidesOtherUsersAnalysisAsNotFound() throws Exception {
        Analysis owned = saveAnalysis(
                owner.getId(),
                "15881234",
                "내 상세 문자",
                RiskLevel.HIGH,
                PhishingCategory.FINANCIAL_INSTITUTION,
                90,
                "2026-07-10T09:00:00+09:00"
        );
        Analysis other = saveAnalysis(
                otherUser.getId(),
                "01033334444",
                "다른 사람 문자",
                RiskLevel.LOW,
                PhishingCategory.OTHER,
                0,
                "2026-07-11T09:00:00+09:00"
        );

        mockMvc.perform(get("/api/v1/analyses/{analysisId}", owned.getId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisId").value(owned.getId()))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"));

        mockMvc.perform(get("/api/v1/analyses/{analysisId}", other.getId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));

        mockMvc.perform(get("/api/v1/analyses/{analysisId}", Long.MAX_VALUE)
                        .header("Authorization", ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesOwnedAnalysisAndFeedbackButCannotDeleteOtherUsersAnalysis() throws Exception {
        Analysis owned = saveAnalysis(
                owner.getId(),
                "15881234",
                "삭제할 문자",
                RiskLevel.HIGH,
                PhishingCategory.FINANCIAL_INSTITUTION,
                90,
                "2026-07-10T09:00:00+09:00"
        );
        Analysis other = saveAnalysis(
                otherUser.getId(),
                "01033334444",
                "다른 사람 문자",
                RiskLevel.LOW,
                PhishingCategory.OTHER,
                0,
                "2026-07-11T09:00:00+09:00"
        );
        analysisFeedbackRepository.save(new AnalysisFeedback(owned, FeedbackType.CORRECT, "정탐"));

        mockMvc.perform(delete("/api/v1/analyses/{analysisId}", other.getId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isNotFound());
        assertTrue(analysisRepository.existsById(other.getId()));

        mockMvc.perform(delete("/api/v1/analyses/{analysisId}", owned.getId())
                        .header("Authorization", ownerToken))
                .andExpect(status().isNoContent());

        assertFalse(analysisRepository.existsById(owned.getId()));
        assertTrue(analysisFeedbackRepository.findByAnalysisId(owned.getId()).isEmpty());
    }

    @Test
    void feedbackCreatesThenUpdatesSingleRowAndChecksOwnership() throws Exception {
        Analysis owned = saveAnalysis(
                owner.getId(),
                "15881234",
                "피드백 문자",
                RiskLevel.HIGH,
                PhishingCategory.FINANCIAL_INSTITUTION,
                90,
                "2026-07-10T09:00:00+09:00"
        );
        Analysis other = saveAnalysis(
                otherUser.getId(),
                "01033334444",
                "다른 사람 문자",
                RiskLevel.LOW,
                PhishingCategory.OTHER,
                0,
                "2026-07-11T09:00:00+09:00"
        );

        mockMvc.perform(post("/api/v1/analyses/{analysisId}/feedback", other.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody("CORRECT", "다른 사용자 피드백")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/analyses/{analysisId}/feedback", owned.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody("CORRECT", "정확한 분석")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/analyses/{analysisId}/feedback", owned.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody("FALSE_POSITIVE", "  실제로는 정상 문자  ")))
                .andExpect(status().isOk());

        AnalysisFeedback feedback = analysisFeedbackRepository.findByAnalysisId(owned.getId()).orElseThrow();
        assertEquals(1, analysisFeedbackRepository.count());
        assertEquals(FeedbackType.FALSE_POSITIVE, feedback.getType());
        assertEquals("실제로는 정상 문자", feedback.getComment());
    }

    @Test
    void listRejectsInvalidDateRangeAndMissingAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/analyses")
                        .header("Authorization", ownerToken)
                        .param("from", "2026-07-11")
                        .param("to", "2026-07-10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/analyses"))
                .andExpect(status().isUnauthorized());
    }

    private Analysis saveAnalysis(
            Long userId,
            String sender,
            String preview,
            RiskLevel riskLevel,
            PhishingCategory category,
            int score,
            String analyzedAt
    ) {
        OffsetDateTime time = OffsetDateTime.parse(analyzedAt);
        return analysisRepository.save(new Analysis(
                userId,
                null,
                sender,
                HASH,
                preview,
                category,
                AnalysisSource.MANUAL,
                0,
                score,
                score,
                riskLevel,
                "테스트 설명",
                time.minusMinutes(1),
                time
        ));
    }

    private String feedbackBody(String type, String comment) {
        return """
                {
                  "type": "%s",
                  "comment": "%s"
                }
                """.formatted(type, comment);
    }
}
