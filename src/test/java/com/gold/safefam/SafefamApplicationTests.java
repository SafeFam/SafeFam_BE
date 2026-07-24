package com.gold.safefam;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 애플리케이션 컨텍스트 기동과 핵심 API의 OpenAPI 문서 노출을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class SafefamApplicationTests {

    @Autowired
    private WebApplicationContext context;

    /** 전체 Spring Bean과 JPA 매핑이 테스트 환경에서 정상 초기화되는지 확인한다. */
    @Test
    void contextLoads() {
    }

    /** 기존 API와 신규 화이트리스트·신고·트렌드 경로가 OpenAPI에 포함되는지 확인한다. */
    @Test
    void openApiDocumentContainsCoreEndpoints() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        String openApiDocument = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(openApiDocument.contains("/api/v1/auth/login"));
        assertTrue(openApiDocument.contains("/api/v1/auth/phone-verifications/send"));
        assertTrue(openApiDocument.contains("/api/v1/auth/phone-verifications/verify"));
        assertTrue(openApiDocument.contains("/api/v1/users/me"));
        assertTrue(openApiDocument.contains("/api/v1/analyses"));
        assertTrue(openApiDocument.contains("/api/v1/statistics/overview"));
        assertTrue(openApiDocument.contains("/api/v1/statistics/trends"));
        assertTrue(openApiDocument.contains("/api/v1/devices"));
        assertTrue(openApiDocument.contains("/api/v1/whitelists"));
        assertTrue(openApiDocument.contains("/api/v1/analyses/{analysisId}/report"));
    }

}
