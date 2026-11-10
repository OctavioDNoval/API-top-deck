package org.example.topdeckapi.src.Security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void ignoresForwardingHeadersWhenProxyIsNotTrusted() {
        MockHttpServletRequest request = request("203.0.113.7");
        request.addHeader("X-Forwarded-For", "6.6.6.6");
        request.addHeader("X-Real-IP", "7.7.7.7");

        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void prefersXRealIpWhenProxyIsTrusted() {
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 198.51.100.2");
        request.addHeader("X-Real-IP", "198.51.100.2");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void usesLastForwardedForEntryWithoutXRealIp() {
        // The first entries are client-controlled; the last one was added by our proxy.
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 198.51.100.2");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void fallsBackToRemoteAddrWhenTrustedButNoHeaders() {
        assertThat(new ClientIpResolver(true).resolve(request("172.18.0.5"))).isEqualTo("172.18.0.5");
    }
}
