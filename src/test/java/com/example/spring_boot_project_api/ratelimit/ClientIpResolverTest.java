package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final RateLimitProperties properties = new RateLimitProperties();

    @Test
    void ignoresForwardedHeaderByDefaultAndUsesRemoteAddr() {
        properties.getProxy().setTrustForwardedHeaders(false);
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        request.setRemoteAddr("10.0.0.55");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.55");
    }

    @Test
    void honorsFirstForwardedEntryWhenTrusted() {
        properties.getProxy().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        request.setRemoteAddr("10.0.0.55");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToRemoteAddrWhenTrustedHeaderIsSpam() {
        properties.getProxy().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        request.setRemoteAddr("10.0.0.55");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.55");
    }

    @Test
    void fallsBackToRemoteAddrWhenNoForwardedHeaderPresent() {
        properties.getProxy().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        request.setRemoteAddr("10.0.0.55");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.55");
    }

    @Test
    void supportsIpv6FirstHopWhenTrusted() {
        properties.getProxy().setTrustForwardedHeaders(true);
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        request.setRemoteAddr("10.0.0.55");
        request.addHeader("X-Forwarded-For", "2001:db8::1");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8::1");
    }
}