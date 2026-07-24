package com.gold.safefam.domain.statistics.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.entity.AnalysisKeyword;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisFeedbackRepository;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.report.repository.PhishingReportRepository;
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
import java.time.YearMonth;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 월 경계·위험도 필터·Top 3/Top 5 순위를 포함한 트렌드 카드 집계를 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class TrendCardFlowTest {

    private static final String HASH = "d".repeat(64);
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PhishingReportRepository reportRepository;

    @Autowired
    private AnalysisFeedbackRepository feedbackRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private User user;
    private String token;

    /** 테스트마다 집계 원본과 사용자 데이터를 초기화하고 인증 토큰을 준비한다. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        reportRepository.deleteAll();
        feedbackRepository.deleteAll();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("01055550000", "encoded-password", "트렌드사용자"));
        token = "Bearer " + jwtUtil.generateAccessToken(user.getId(), user.getRole());
    }

    /** 월간 중·고위험 표본만으로 Top 3 유형과 Top 5 표준 키워드가 계산되는지 검증한다. */
    @Test
    void returnsMonthlyAnonymousTopCategoriesAndRiskKeywords() throws Exception {
        YearMonth month = YearMonth.now(SERVICE_ZONE);
        OffsetDateTime currentMonth = month.atDay(2).atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
        OffsetDateTime previousMonth = month.minusMonths(1)
                .atDay(2)
                .atStartOfDay(SERVICE_ZONE)
                .toOffsetDateTime();

        saveMany(3, PhishingCategory.FINANCIAL_INSTITUTION, RiskLevel.HIGH, currentMonth,
                new IndicatorType[]{IndicatorType.IMPERSONATION, IndicatorType.FINANCIAL_ACTION},
                "이체", "계좌 정지", "수수료");
        saveMany(2, PhishingCategory.LOAN, RiskLevel.MEDIUM, currentMonth,
                new IndicatorType[]{IndicatorType.FINANCIAL_ACTION, IndicatorType.URGENCY},
                "이체", "대출");
        saveMany(1, PhishingCategory.GOVERNMENT_AGENCY, RiskLevel.HIGH, currentMonth,
                new IndicatorType[]{IndicatorType.IMPERSONATION, IndicatorType.SENSITIVE_INFORMATION},
                "이체", "인증번호");
        saveMany(1, PhishingCategory.OTHER, RiskLevel.HIGH, currentMonth,
                new IndicatorType[]{IndicatorType.URGENCY},
                "미납");
        saveMany(1, PhishingCategory.DELIVERY, RiskLevel.LOW, currentMonth,
                new IndicatorType[]{IndicatorType.URGENCY},
                "택배");
        saveMany(4, PhishingCategory.JOB, RiskLevel.HIGH, previousMonth,
                new IndicatorType[]{IndicatorType.SENSITIVE_INFORMATION},
                "송금");

        mockMvc.perform(get("/api/v1/statistics/trends")
                        .header("Authorization", token)
                        .param("month", month.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(month.toString()))
                .andExpect(jsonPath("$.data.sampleSize").value(7))
                .andExpect(jsonPath("$.data.topPhishingTypes.length()").value(3))
                .andExpect(jsonPath("$.data.topPhishingTypes[0].rank").value(1))
                .andExpect(jsonPath("$.data.topPhishingTypes[0].category")
                        .value("FINANCIAL_INSTITUTION"))
                .andExpect(jsonPath("$.data.topPhishingTypes[0].count").value(3))
                .andExpect(jsonPath("$.data.topPhishingTypes[1].category").value("LOAN"))
                .andExpect(jsonPath("$.data.topPhishingTypes[1].count").value(2))
                .andExpect(jsonPath("$.data.topPhishingTypes[2].category")
                        .value("GOVERNMENT_AGENCY"))
                .andExpect(jsonPath("$.data.topRiskKeywords.length()").value(5))
                .andExpect(jsonPath("$.data.topRiskKeywords[0].keyword").value("이체"))
                .andExpect(jsonPath("$.data.topRiskKeywords[0].count").value(6));
    }

    /** 잘못된 월 형식과 미인증 요청이 각각 400과 401로 거절되는지 검증한다. */
    @Test
    void rejectsInvalidMonthAndUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/trends")
                        .header("Authorization", token)
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/statistics/trends"))
                .andExpect(status().isUnauthorized());
    }

    /** 지정한 월·등급·유형·키워드를 가진 익명 집계용 분석 표본을 반복 저장한다. */
    private void saveMany(
            int count,
            PhishingCategory category,
            RiskLevel riskLevel,
            OffsetDateTime analyzedAt,
            IndicatorType[] indicators,
            String... keywords
    ) {
        for (int index = 0; index < count; index++) {
            Analysis analysis = new Analysis(
                    user.getId(),
                    null,
                    "테스트 발신자",
                    HASH,
                    "익명 집계용 미리보기",
                    category,
                    AnalysisSource.AUTO,
                    0,
                    riskLevel == RiskLevel.LOW ? 10 : 70,
                    riskLevel == RiskLevel.HIGH ? 90 : 60,
                    riskLevel,
                    "트렌드 테스트 설명",
                    analyzedAt.minusMinutes(1),
                    analyzedAt.plusMinutes(index)
            );
            for (IndicatorType indicator : indicators) {
                analysis.addIndicator(new AnalysisIndicator(indicator, "테스트 탐지 근거"));
            }
            for (String keyword : keywords) {
                analysis.addKeyword(new AnalysisKeyword(keyword));
            }
            analysisRepository.save(analysis);
        }
    }
}
