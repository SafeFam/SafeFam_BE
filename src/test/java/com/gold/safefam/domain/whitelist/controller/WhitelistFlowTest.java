package com.gold.safefam.domain.whitelist.controller;

import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.domain.whitelist.repository.WhitelistRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 화이트리스트 정규화·중복·사용자 경계와 프리패스 확인 흐름을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class WhitelistFlowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private WhitelistRepository whitelistRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String ownerToken;
    private String otherToken;

    /** 테스트마다 사용자·토큰·화이트리스트 저장소를 독립된 상태로 초기화한다. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        whitelistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(new User("01011110000", "encoded-password", "소유자"));
        User other = userRepository.save(new User("01022220000", "encoded-password", "다른사용자"));
        ownerToken = "Bearer " + jwtUtil.generateAccessToken(owner.getId(), owner.getRole());
        otherToken = "Bearer " + jwtUtil.generateAccessToken(other.getId(), other.getRole());
    }

    /** 번호 정규화, 사용자별 중복, 목록, 소유권 삭제, 프리패스 확인을 한 흐름으로 검증한다. */
    @Test
    void managesOwnedWhitelistAndChecksNormalizedSender() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/whitelists")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sender": "+82 (10) 1234-5678",
                                  "label": " 주거래 은행 "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sender").value("01012345678"))
                .andExpect(jsonPath("$.data.label").value("주거래 은행"))
                .andReturn();
        Number whitelistId = JsonPath.read(
                created.getResponse().getContentAsString(),
                "$.data.whitelistId"
        );

        mockMvc.perform(get("/api/v1/whitelists/check")
                        .header("Authorization", ownerToken)
                        .param("sender", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sender").value("01012345678"))
                .andExpect(jsonPath("$.data.whitelisted").value(true));

        mockMvc.perform(post("/api/v1/whitelists")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sender": "010 1234 5678"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/whitelists")
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sender": "010-1234-5678"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/whitelists")
                        .header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/v1/whitelists/{whitelistId}", whitelistId)
                        .header("Authorization", otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/whitelists/{whitelistId}", whitelistId)
                        .header("Authorization", ownerToken))
                .andExpect(status().isNoContent());

        assertEquals(1, whitelistRepository.count());
    }

    /** 인증 토큰이 없는 화이트리스트 접근이 401로 거절되는지 검증한다. */
    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/whitelists"))
                .andExpect(status().isUnauthorized());
    }
}
