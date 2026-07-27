package com.gold.safefam.domain.family.controller;

import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 초대 생성 → 수락 → 목록 조회 → 해제 전체 플로우를 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FamilyFlowTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FamilyLinkRepository familyLinkRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String guardianToken;
    private String wardToken;
    private Long guardianId;
    private Long wardId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        familyLinkRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User guardian = userRepository.save(new User("01011112222", "encoded-password", "보호자"));
        User ward = userRepository.save(new User("01033334444", "encoded-password", "피보호자"));

        guardianId = guardian.getId();
        wardId = ward.getId();
        guardianToken = jwtUtil.generateAccessToken(guardian.getId(), guardian.getRole());
        wardToken = jwtUtil.generateAccessToken(ward.getId(), ward.getRole());
    }

    @Test
    void fullFlow_invite_link_list_revoke() throws Exception {
        // 1. 보호자가 초대 코드 생성
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/family/invite")
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.inviteCode").isString())
                .andExpect(jsonPath("$.data.qrToken").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andReturn();

        String inviteCode = com.jayway.jsonpath.JsonPath.read(
                inviteResult.getResponse().getContentAsString(),
                "$.data.inviteCode"
        );

        // 2. 피보호자가 초대 코드로 연결 수락
        mockMvc.perform(post("/api/v1/family/link/code")
                        .header("Authorization", "Bearer " + wardToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "inviteCode": "%s" }
                                """.formatted(inviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 3. DB에서 ACTIVE 상태 확인
        transactionTemplate.executeWithoutResult(status -> {
            assertEquals(1, familyLinkRepository.findAllByProtectorIdAndStatus(
                    guardianId, FamilyLinkStatus.ACTIVE).size());
        });

        // 4. 보호자가 가족 목록 조회
        MvcResult listResult = mockMvc.perform(get("/api/v1/family/members")
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].wardId").value(wardId))
                .andReturn();

        String linkIdStr = com.jayway.jsonpath.JsonPath.read(
                listResult.getResponse().getContentAsString(),
                "$.data[0].linkId"
        ).toString();

        // 5. 보호자가 연결 해제
        mockMvc.perform(delete("/api/v1/family/" + linkIdStr)
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isNoContent());

        // 6. REVOKED 상태 확인
        transactionTemplate.executeWithoutResult(status -> {
            FamilyLink link = familyLinkRepository.findById(Long.parseLong(linkIdStr)).orElseThrow();
            assertEquals(FamilyLinkStatus.REVOKED, link.getStatus());
        });
    }

    @Test
    void expiredInviteCode_isRejected() throws Exception {
        // 만료된 초대 코드 직접 삽입
        transactionTemplate.executeWithoutResult(status -> {
            User guardian = userRepository.findById(guardianId).orElseThrow();
            FamilyLink link = FamilyLink.createInvite(
                    guardian,
                    "999999",
                    "expired-qr-token",
                    java.time.OffsetDateTime.now().minusMinutes(1)
            );
            familyLinkRepository.save(link);
        });

        mockMvc.perform(post("/api/v1/family/link/code")
                        .header("Authorization", "Bearer " + wardToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "inviteCode": "999999" }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void selfLink_isRejected() throws Exception {
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/family/invite")
                        .header("Authorization", "Bearer " + guardianToken))
                .andExpect(status().isCreated())
                .andReturn();

        String inviteCode = com.jayway.jsonpath.JsonPath.read(
                inviteResult.getResponse().getContentAsString(),
                "$.data.inviteCode"
        );

        // 보호자가 본인 코드로 수락 시도
        mockMvc.perform(post("/api/v1/family/link/code")
                        .header("Authorization", "Bearer " + guardianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "inviteCode": "%s" }
                                """.formatted(inviteCode)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unauthorizedRevoke_isForbidden() throws Exception {
        // 관계 없는 제3자 생성
        User stranger = userRepository.save(new User("01055556666", "encoded-password", "제3자"));
        String strangerToken = jwtUtil.generateAccessToken(stranger.getId(), stranger.getRole());

        // 보호자-피보호자 연결 생성
        transactionTemplate.executeWithoutResult(status -> {
            User guardian = userRepository.findById(guardianId).orElseThrow();
            User ward = userRepository.findById(wardId).orElseThrow();
            FamilyLink link = FamilyLink.createInvite(
                    guardian, "123456", "some-qr-token",
                    java.time.OffsetDateTime.now().plusMinutes(10)
            );
            link.accept(ward);
            familyLinkRepository.save(link);
        });

        Long linkId = familyLinkRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/v1/family/" + linkId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(post("/api/v1/family/invite"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guardianCanViewWardLogs() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            User guardian = userRepository.findById(guardianId).orElseThrow();
            User ward = userRepository.findById(wardId).orElseThrow();
            FamilyLink link = FamilyLink.createInvite(
                    guardian, "123456", "some-qr-token",
                    java.time.OffsetDateTime.now().plusMinutes(10)
            );
            link.accept(ward);
            familyLinkRepository.save(link);
        });

        mockMvc.perform(get("/api/v1/family/ward/" + wardId + "/logs")
                        .header("Authorization", "Bearer " + guardianToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void strangerCannotViewWardLogs() throws Exception {
        // 보호자-피보호자 실제 연결 생성
        transactionTemplate.executeWithoutResult(status -> {
            User guardian = userRepository.findById(guardianId).orElseThrow();
            User ward = userRepository.findById(wardId).orElseThrow();
            FamilyLink link = FamilyLink.createInvite(
                    guardian, "123456", "some-qr-token",
                    java.time.OffsetDateTime.now().plusMinutes(10)
            );
            link.accept(ward);
            familyLinkRepository.save(link);
        });

        User stranger = userRepository.save(new User("01055556666", "encoded-password", "제3자"));
        String strangerToken = jwtUtil.generateAccessToken(stranger.getId(), stranger.getRole());

        mockMvc.perform(get("/api/v1/family/ward/" + wardId + "/logs")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }
}