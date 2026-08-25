package com.gold.safefam.global.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class SecurityErrorHandlerTest {

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void accessDeniedResponseUsesCommonApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("forbidden")
        );

        assertEquals(403, response.getStatus());
        assertEquals("ERROR", JsonPath.read(response.getContentAsString(), "$.status"));
        assertEquals("AU006", JsonPath.read(response.getContentAsString(), "$.code"));
        assertEquals("접근 권한이 없습니다.",
                JsonPath.read(response.getContentAsString(), "$.message"));
    }

    @Test
    void authenticationEntryPointIncludesUnauthorizedCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new AuthenticationCredentialsNotFoundException("unauthorized")
        );

        assertEquals(401, response.getStatus());
        assertEquals("ERROR", JsonPath.read(response.getContentAsString(), "$.status"));
        assertEquals("AU001", JsonPath.read(response.getContentAsString(), "$.code"));
        assertEquals("인증이 필요합니다.",
                JsonPath.read(response.getContentAsString(), "$.message"));
    }
}
