package com.gold.safefam.domain.family.safety.controller;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyNotificationTarget;
import com.gold.safefam.domain.family.safety.entity.FamilySafetyCase;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import com.gold.safefam.domain.family.safety.repository.FamilySafetyCaseRepository;
import com.gold.safefam.domain.family.safety.service.FamilySafetyCaseService;
import com.gold.safefam.domain.family.service.FamilyNotificationService;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HIGH 탐지부터 공동 대응 건 생성, 권한 검증, 전화, 최종 처리와 재알림까지
 * 가족 안전 기능의 핵심 사용자 흐름을 Spring 통합 환경에서 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FamilySafetyCaseFlowTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FamilySafetyCaseRepository safetyCaseRepository;
    @Autowired private FamilySafetyCaseService safetyCaseService;
    @Autowired private FamilyNotificationService familyNotificationService;
    @Autowired private FamilyLinkRepository familyLinkRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private Long guardianId;
    private Long wardId;
    private String guardianToken;
    private Long analysisId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        safetyCaseRepository.deleteAll();
        familyLinkRepository.deleteAll();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User guardian = userRepository.save(
                new User("01011112222", "encoded-password", "보호자")
        );
        User ward = userRepository.save(
                new User("01033334444", "encoded-password", "어머니")
        );
        guardianId = guardian.getId();
        wardId = ward.getId();
        guardianToken = jwtUtil.generateAccessToken(guardianId, guardian.getRole());

        FamilyLink link = FamilyLink.createInvite(
                guardian,
                "123456",
                "safety-test-qr-token",
                OffsetDateTime.now().plusMinutes(10)
        );
        link.accept(ward);
        familyLinkRepository.save(link);

        analysisId = saveHighAnalysis(wardId).getId();
    }

    @Test
    void highRiskCreatesOneSharedCaseAndGuardianCompletesSafetyFlow() throws Exception {
        familyNotificationService.mirrorHighRiskToGuardians(wardId, "계좌 110-1234-567890 송금 유도", analysisId);
        familyNotificationService.mirrorHighRiskToGuardians(wardId, "중복 이벤트", analysisId);

        List<FamilySafetyCase> cases = safetyCaseRepository.findAll();
        assertEquals(1, cases.size());
        Long caseId = cases.get(0).getId();

        mockMvc.perform(get("/api/v1/family/alerts")
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].caseId").value(caseId));

        mockMvc.perform(get("/api/v1/family/alerts")
                        .header("Authorization", "Bearer " + guardianToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        mockMvc.perform(get("/api/v1/family/alerts/{caseId}", caseId)
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.riskScore").value(91))
                .andExpect(jsonPath("$.data.wardName").value("어머니"))
                .andExpect(jsonPath("$.data.maskedMessagePreview").value(
                        containsString("[PHONE]")))
                .andExpect(jsonPath("$.data.maskedMessagePreview").value(
                        not(containsString("010-9876-5432"))))
                .andExpect(jsonPath("$.data.riskSummary").value(containsString("[ACCOUNT]")))
                .andExpect(jsonPath("$.data.riskSummary").value(
                        not(containsString("110-1234-567890"))));

        mockMvc.perform(post("/api/v1/family/alerts/{caseId}/call", caseId)
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("01033334444"));

        mockMvc.perform(patch("/api/v1/family/alerts/{caseId}/status", caseId)
                        .header("Authorization", "Bearer " + guardianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "SAFE_CONFIRMED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SAFE_CONFIRMED"))
                .andExpect(jsonPath("$.data.handledById").value(guardianId))
                .andExpect(jsonPath("$.data.nextReminderAt").doesNotExist());

        FamilySafetyCase resolved = safetyCaseRepository.findById(caseId).orElseThrow();
        assertEquals(FamilySafetyStatus.SAFE_CONFIRMED, resolved.getStatus());
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getNextReminderAt()).isNull();
    }

    @Test
    void unrelatedUserCannotReadOrHandleSafetyCase() throws Exception {
        FamilySafetyNotificationTarget target = safetyCaseService.createForHighRisk(wardId, analysisId).target();
        User stranger = userRepository.save(
                new User("01055556666", "encoded-password", "관계없는 사용자")
        );
        String strangerToken = jwtUtil.generateAccessToken(stranger.getId(), stranger.getRole());

        mockMvc.perform(get("/api/v1/family/alerts/{caseId}", target.caseId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/family/alerts/{caseId}/call", target.caseId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/family/alerts/{caseId}", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unresolvedCaseIsClaimedForReminderButResolvedCaseIsNot() {
        FamilySafetyNotificationTarget target = safetyCaseService.createForHighRisk(wardId, analysisId).target();
        FamilySafetyCase current = safetyCaseRepository.findById(target.caseId()).orElseThrow();

        Analysis secondAnalysis = saveHighAnalysis(wardId);
        User ward = userRepository.findById(wardId).orElseThrow();
        FamilySafetyCase dueCase = FamilySafetyCase.create(
                secondAnalysis,
                ward,
                "국민은행",
                "외부 링크 클릭 유도",
                "[URL] 접속 요청",
                "금융기관 사칭",
                OffsetDateTime.now().minusMinutes(2),
                OffsetDateTime.now().minusMinutes(1)
        );
        safetyCaseRepository.saveAndFlush(dueCase);

        safetyCaseService.resolve(guardianId, current.getId(), FamilySafetyStatus.SAFE_CONFIRMED);
        FamilySafetyCase resolved = safetyCaseRepository.findById(current.getId()).orElseThrow();
        assertThat(resolved.getNextReminderAt()).isNull();

        List<FamilySafetyNotificationTarget> reminders = safetyCaseService.claimDueReminders();

        assertThat(reminders).extracting(FamilySafetyNotificationTarget::caseId)
                .containsExactly(dueCase.getId());
        FamilySafetyCase claimed = safetyCaseRepository.findById(dueCase.getId()).orElseThrow();
        assertEquals(1, claimed.getReminderCount());
        assertThat(claimed.getNextReminderAt()).isAfter(OffsetDateTime.now());
        assertThat(claimed.getNextReminderAt()).isBefore(OffsetDateTime.now().plusMinutes(2));

        safetyCaseService.recordReminderDelivered(dueCase.getId());
        FamilySafetyCase delivered = safetyCaseRepository.findById(dueCase.getId()).orElseThrow();
        assertThat(delivered.getLastNotifiedAt()).isNotNull();
        assertThat(delivered.getNextReminderAt()).isAfter(OffsetDateTime.now().plusMinutes(5));
    }

    @Test
    void pendingIsRejectedAsCompletionStatus() throws Exception {
        FamilySafetyNotificationTarget target = safetyCaseService.createForHighRisk(wardId, analysisId).target();

        mockMvc.perform(patch("/api/v1/family/alerts/{caseId}/status", target.caseId())
                        .header("Authorization", "Bearer " + guardianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "PENDING" }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/family/alerts")
                        .header("Authorization", "Bearer " + guardianToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/family/alerts")
                        .header("Authorization", "Bearer " + guardianToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private Analysis saveHighAnalysis(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        Analysis analysis = new Analysis(
                userId,
                "safety-" + UUID.randomUUID(),
                "010-9876-5432",
                "a".repeat(64),
                "상담 전화 010-9876-5432, 계좌 110-1234-567890",
                PhishingCategory.FINANCIAL_INSTITUTION,
                AnalysisSource.AUTO,
                90,
                85,
                91,
                RiskLevel.HIGH,
                "계좌 110-1234-567890 송금과 외부 링크 클릭을 유도합니다.",
                now.minusSeconds(1),
                now
        );
        analysis.addIndicator(new AnalysisIndicator(
                IndicatorType.FINANCIAL_ACTION,
                "계좌 110-1234-567890 송금 유도"
        ));
        analysis.addIndicator(new AnalysisIndicator(
                IndicatorType.MALICIOUS_URL,
                "외부 링크 클릭 유도"
        ));
        return analysisRepository.saveAndFlush(analysis);
    }
}
