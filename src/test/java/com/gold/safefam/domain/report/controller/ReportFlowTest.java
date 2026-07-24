package com.gold.safefam.domain.report.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisFeedbackRepository;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.report.entity.PhishingReport;
import com.gold.safefam.domain.report.repository.PhishingReportRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.security.JwtUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 신고 소유권·멱등성과 비식별 스냅샷 저장을 통합 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class ReportFlowTest {

    private static final String HASH = "c".repeat(64);

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
    private User owner;
    private String ownerToken;
    private String otherToken;

    /** 테스트마다 신고·분석·사용자 데이터를 지우고 서로 다른 소유자의 토큰을 준비한다. */
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

        owner = userRepository.save(new User("01033330000", "encoded-password", "신고자"));
        User other = userRepository.save(new User("01044440000", "encoded-password", "다른사용자"));
        ownerToken = "Bearer " + jwtUtil.generateAccessToken(owner.getId(), owner.getRole());
        otherToken = "Bearer " + jwtUtil.generateAccessToken(other.getId(), other.getRole());
    }

    /** 익명 스냅샷 저장, 재신고 멱등성, 다른 사용자 분석의 비노출을 검증한다. */
    @Test
    void storesAnonymousSnapshotAndReturnsExistingReportForRetry() throws Exception {
        Analysis analysis = saveAnalysis(owner.getId());

        MvcResult first = mockMvc.perform(post("/api/v1/analyses/{analysisId}/report", analysis.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "PHISHING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.analysisId").value(analysis.getId()))
                .andExpect(jsonPath("$.data.category").value("FINANCIAL_INSTITUTION"))
                .andReturn();
        Number firstReportId = JsonPath.read(
                first.getResponse().getContentAsString(),
                "$.data.reportId"
        );

        mockMvc.perform(post("/api/v1/analyses/{analysisId}/report", analysis.getId())
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "SPAM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(firstReportId));

        mockMvc.perform(post("/api/v1/analyses/{analysisId}/report", analysis.getId())
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "PHISHING"}
                                """))
                .andExpect(status().isNotFound());

        PhishingReport stored = reportRepository.findAll().get(0);
        assertEquals(1, reportRepository.count());
        assertEquals(HASH, stored.getContentHash());
        assertEquals("계좌 정지 [URL] 010-****-****", stored.getContentPreview());
        assertFalse(stored.getContentPreview().contains("010-1234-5678"));
    }

    /** 원문 대신 보호된 해시와 마스킹 미리보기가 있는 분석 이력을 준비한다. */
    private Analysis saveAnalysis(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return analysisRepository.save(new Analysis(
                userId,
                null,
                "국민은행",
                HASH,
                "계좌 정지 [URL] 010-****-****",
                PhishingCategory.FINANCIAL_INSTITUTION,
                AnalysisSource.MANUAL,
                0,
                80,
                90,
                RiskLevel.HIGH,
                "테스트 설명",
                now.minusMinutes(1),
                now
        ));
    }
}
