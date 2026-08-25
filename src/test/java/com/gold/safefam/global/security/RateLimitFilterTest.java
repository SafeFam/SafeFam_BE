package com.gold.safefam.global.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
    }

    @Test
    @DisplayName("POST /login 제한 횟수 이하 요청은 통과한다")
    void allowsLoginRequestsUnderLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("POST /login 제한 횟수 초과 요청은 429를 반환한다")
    void blocksLoginRequestsOverLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("192.168.0.1");

        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"code\":\"C002\"");
        assertThat(response.getContentAsString()).contains("요청 한도를 초과했습니다");
    }

    @Test
    @DisplayName("POST /phone-verifications/send 제한 횟수 초과 요청은 429를 반환한다")
    void blocksPhoneVerificationRequestsOverLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/phone-verifications/send");
        request.setRemoteAddr("10.0.0.2");

        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("요청 한도를 초과했습니다");
    }

    @Test
    @DisplayName("GET 요청은 rate limit 대상이 아니다")
    void doesNotLimitGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.3");

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("rate limit 대상이 아닌 경로는 제한하지 않는다")
    void doesNotLimitNonTargetPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/user/profile");
        request.setRemoteAddr("10.0.0.4");

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("POST /analyses 제한 횟수 이하 요청은 통과한다")
    void allowsAnalysisRequestsUnderLimit() throws Exception {
        setUpSecurityContext(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/analyses");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("POST /analyses 제한 횟수 초과 요청은 429를 반환한다")
    void blocksAnalysisRequestsOverLimit() throws Exception {
        setUpSecurityContext(2L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/analyses");

        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"code\":\"AN002\"");
        assertThat(response.getContentAsString()).contains("분석 요청 한도를 초과했습니다");
    }

    @Test
    @DisplayName("서로 다른 사용자는 독립적으로 rate limit이 적용된다")
    void rateLimitIsIsolatedPerUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/analyses");

        // user3 10회 소진
        setUpSecurityContext(3L);
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        // user4는 영향 없어야 함
        setUpSecurityContext(4L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void setUpSecurityContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
