package com.gold.safefam.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
    }

    @Test
    @DisplayName("제한 횟수 이하 요청은 통과한다")
    void allowsRequestsUnderLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("제한 횟수 초과 요청은 429를 반환한다")
    void blocksRequestsOverLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("192.168.0.1");

        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("rate limit 대상이 아닌 경로는 제한하지 않는다")
    void doesNotLimitNonTargetPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/profile");
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}