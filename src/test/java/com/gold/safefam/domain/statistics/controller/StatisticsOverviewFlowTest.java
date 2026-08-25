package com.gold.safefam.domain.statistics.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 통계 API의 기간 필터·사용자 경계·0건 분포 반환을 통합 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class StatisticsOverviewFlowTest {

    private static final String HASH = "b".repeat(64);

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

        owner = userRepository.save(new User("01055556666", "encoded-password", "통계사용자"));
        otherUser = userRepository.save(new User("01077778888", "encoded-password", "다른사용자"));
        ownerToken = "Bearer " + jwtUtil.generateAccessToken(owner.getId(), owner.getRole());
    }

    /** 기본 30일 조회가 내 데이터만 집계하고 빈 항목도 0건으로 반환하는지 확인한다. */
    @Test
    void defaultPeriodReturnsOnlyOwnedRecentAnalysesAndIncludesZeroBuckets() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        saveAnalysis(owner.getId(), RiskLevel.HIGH, PhishingCategory.FINANCIAL_INSTITUTION, now.minusDays(1));
        saveAnalysis(owner.getId(), RiskLevel.LOW, PhishingCategory.DELIVERY, now.minusDays(10));
        saveAnalysis(owner.getId(), RiskLevel.HIGH, PhishingCategory.GOVERNMENT_AGENCY, now.minusDays(31));
        saveAnalysis(otherUser.getId(), RiskLevel.HIGH, PhishingCategory.LOAN, now.minusDays(1));

        mockMvc.perform(get("/api/v1/statistics/overview")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.period").value("LAST_30_DAYS"))
                .andExpect(jsonPath("$.data.totalAnalysisCount").value(2))
                .andExpect(jsonPath("$.data.highRiskCount").value(1))
                .andExpect(jsonPath("$.data.riskDistribution.length()").value(3))
                .andExpect(jsonPath("$.data.riskDistribution[0].riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.riskDistribution[0].count").value(1))
                .andExpect(jsonPath("$.data.riskDistribution[1].riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.riskDistribution[1].count").value(0))
                .andExpect(jsonPath("$.data.riskDistribution[2].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.riskDistribution[2].count").value(1))
                .andExpect(jsonPath("$.data.categoryDistribution.length()").value(7))
                .andExpect(jsonPath("$.data.categoryDistribution[0].category").value("FINANCIAL_INSTITUTION"))
                .andExpect(jsonPath("$.data.categoryDistribution[0].count").value(1))
                .andExpect(jsonPath("$.data.categoryDistribution[1].count").value(0))
                .andExpect(jsonPath("$.data.categoryDistribution[4].category").value("DELIVERY"))
                .andExpect(jsonPath("$.data.categoryDistribution[4].count").value(1));
    }

    /** 7일·30일·90일·전체 선택에 따라 포함되는 탐지 범위가 달라지는지 확인한다. */
    @Test
    void selectedPeriodFiltersAnalysesFromCurrentTime() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        saveAnalysis(owner.getId(), RiskLevel.MEDIUM, PhishingCategory.MESSENGER, now.minusDays(6));
        saveAnalysis(owner.getId(), RiskLevel.LOW, PhishingCategory.LOAN, now.minusDays(8));
        saveAnalysis(owner.getId(), RiskLevel.HIGH, PhishingCategory.GOVERNMENT_AGENCY, now.minusDays(45));
        saveAnalysis(owner.getId(), RiskLevel.HIGH, PhishingCategory.OTHER, now.minusDays(100));

        expectPeriodCounts("LAST_7_DAYS", 1, 0);
        expectPeriodCounts("LAST_30_DAYS", 2, 0);
        expectPeriodCounts("LAST_90_DAYS", 3, 1);
        expectPeriodCounts("ALL", 4, 2);
    }

    /** 처리 중·실패·유형 미분류 이력이 있어도 기간별 집계가 null 그룹에서 실패하지 않는지 확인한다. */
    @Test
    void nullResultFieldsAreExcludedFromEveryStatisticsPeriod() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        savePendingAnalysis(owner.getId(), now.minusHours(3));
        saveFailedAnalysis(owner.getId(), now.minusHours(2));
        saveAnalysis(owner.getId(), RiskLevel.HIGH, null, now.minusHours(1));
        saveAnalysis(owner.getId(), RiskLevel.LOW, PhishingCategory.DELIVERY, now.minusMinutes(30));

        for (String period : new String[]{
                "LAST_7_DAYS",
                "LAST_30_DAYS",
                "LAST_90_DAYS",
                "ALL"
        }) {
            mockMvc.perform(get("/api/v1/statistics/overview")
                            .header("Authorization", ownerToken)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.period").value(period))
                    .andExpect(jsonPath("$.data.totalAnalysisCount").value(2))
                    .andExpect(jsonPath("$.data.highRiskCount").value(1))
                    .andExpect(jsonPath("$.data.categoryDistribution[4].category")
                            .value("DELIVERY"))
                    .andExpect(jsonPath("$.data.categoryDistribution[4].count")
                            .value(1));
        }
    }

    /** 분석 이력이 없어도 모든 위험 등급과 피싱 유형을 0건으로 반환하는지 확인한다. */
    @Test
    void emptyHistoryReturnsAllBucketsWithZeroCounts() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/overview")
                        .header("Authorization", ownerToken)
                        .param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAnalysisCount").value(0))
                .andExpect(jsonPath("$.data.highRiskCount").value(0))
                .andExpect(jsonPath("$.data.riskDistribution.length()").value(3))
                .andExpect(jsonPath("$.data.riskDistribution[0].count").value(0))
                .andExpect(jsonPath("$.data.riskDistribution[1].count").value(0))
                .andExpect(jsonPath("$.data.riskDistribution[2].count").value(0))
                .andExpect(jsonPath("$.data.categoryDistribution.length()").value(7))
                .andExpect(jsonPath("$.data.categoryDistribution[0].count").value(0))
                .andExpect(jsonPath("$.data.categoryDistribution[6].count").value(0));
    }

    /** 인증 정보가 없는 요청과 지원하지 않는 기간 값이 거절되는지 확인한다. */
    @Test
    void invalidOrUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/statistics/overview")
                        .header("Authorization", ownerToken)
                        .param("period", "LAST_YEAR"))
                .andExpect(status().isBadRequest());
    }

    /** 특정 기간의 총 탐지 수와 고위험 탐지 수를 공통 방식으로 검증한다. */
    private void expectPeriodCounts(String period, long totalCount, long highRiskCount) throws Exception {
        mockMvc.perform(get("/api/v1/statistics/overview")
                        .header("Authorization", ownerToken)
                        .param("period", period))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value(period))
                .andExpect(jsonPath("$.data.totalAnalysisCount").value(totalCount))
                .andExpect(jsonPath("$.data.highRiskCount").value(highRiskCount));
    }

    /** 집계 조건을 제어할 수 있도록 사용자·위험 등급·유형·분석 시각을 지정해 저장한다. */
    private void saveAnalysis(
            Long userId,
            RiskLevel riskLevel,
            PhishingCategory category,
            OffsetDateTime analyzedAt
    ) {
        analysisRepository.save(new Analysis(
                userId,
                null,
                "15881234",
                HASH,
                "통계 테스트 문자",
                category,
                AnalysisSource.MANUAL,
                0,
                0,
                riskLevel == RiskLevel.HIGH ? 90 : 20,
                riskLevel,
                "통계 테스트 설명",
                analyzedAt.minusMinutes(1),
                analyzedAt
        ));
    }

    private void savePendingAnalysis(Long userId, OffsetDateTime receivedAt) {
        analysisRepository.save(Analysis.pending(
                userId,
                "pending-message",
                "15881234",
                HASH,
                "처리 중인 통계 테스트 문자",
                AnalysisSource.MANUAL,
                receivedAt
        ));
    }

    private void saveFailedAnalysis(Long userId, OffsetDateTime analyzedAt) {
        Analysis analysis = Analysis.pending(
                userId,
                "failed-message",
                "15881234",
                HASH,
                "실패한 통계 테스트 문자",
                AnalysisSource.MANUAL,
                analyzedAt.minusMinutes(1)
        );
        analysis.fail("PIPELINE_FAILED", analyzedAt);
        analysisRepository.save(analysis);
    }
}
