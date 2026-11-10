package org.example.topdeckapi.src.Security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new ClientIpResolver(false));
    }

    @AfterEach
    void tearDown() {
        filter.shutdown();
    }

    private int call(String method, String uri, String remoteAddr, String spoofedIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        if (spoofedIp != null) {
            request.addHeader("X-Forwarded-For", spoofedIp);
            request.addHeader("X-Real-IP", spoofedIp);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void authAllowsTenPerMinuteThenBlocks() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("POST", "/auth/login", "203.0.113.1", null)).isEqualTo(200);
        }
        assertThat(call("POST", "/auth/login", "203.0.113.1", null)).isEqualTo(429);
    }

    @Test
    void spoofedHeadersDoNotResetTheLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            call("POST", "/auth/login", "203.0.113.2", "10.0.0." + i);
        }
        assertThat(call("POST", "/auth/login", "203.0.113.2", "10.0.0.99")).isEqualTo(429);
    }

    @Test
    void limitsAreCountedPerClient() throws Exception {
        for (int i = 0; i < 11; i++) {
            call("POST", "/auth/login", "203.0.113.3", null);
        }
        assertThat(call("POST", "/auth/login", "203.0.113.4", null)).isEqualTo(200);
    }

    @Test
    void guestOrdersAllowFiveThenBlockWithRetryAfter() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(call("POST", "/pedidos/public/pedidoEfimero", "203.0.113.5", null)).isEqualTo(200);
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pedidos/public/pedidoEfimero");
        request.setRemoteAddr("203.0.113.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isPositive();
    }

    @Test
    void otherRoutesAreNotLimited() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertThat(call("GET", "/products/public/ofertas", "203.0.113.6", null)).isEqualTo(200);
        }
        // GET on the order path is not the guest-order rule.
        for (int i = 0; i < 10; i++) {
            assertThat(call("GET", "/pedidos/public/pedidoEfimero", "203.0.113.6", null)).isEqualTo(200);
        }
    }
}
