package com.gold.safefam.global.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class SecurityErrorHandlerTest {

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

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
        assertEquals("접근 권한이 없습니다.",
                JsonPath.read(response.getContentAsString(), "$.message"));
    }
}
