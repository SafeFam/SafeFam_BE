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

@SpringBootTest
@ActiveProfiles("test")
class SafefamApplicationTests {

    @Autowired
    private WebApplicationContext context;

    @Test
    void contextLoads() {
    }

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
        assertTrue(openApiDocument.contains("/api/v1/devices"));
    }

}
