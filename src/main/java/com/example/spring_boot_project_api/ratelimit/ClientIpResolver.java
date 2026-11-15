package com.example.spring_boot_project_api.ratelimit;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the effective client IP for rate limiting and audit logging.
 *
 * <p>Security stance: forwarded headers are <b>never</b> trusted by default
 * because a client can send {@code X-Forwarded-For: <victim-ip>} and make an
 * attacker's requests count against the victim (or smear the limiter). Only
 * when {@code app.ratelimit.proxy.trust-forwarded-headers=true}
 * (i.e. the server sits behind a strict reverse proxy that overwrites these
 * headers) is the first hop of the configured header honored.
 */
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile(
            "^[0-9a-fA-F:]+(:(([0-9a-fA-F]{1,4}:){0,6}([0-9a-fA-F]{1,4}|\"?\\d{1,3}(\\.\\d{1,3}){3}\"?)|::))?$");

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the best-effort client IP for the request. Falls back to
     * {@code request.getRemoteAddr()} whenever the forwarded header is missing,
     * malformed, or disabled.
     */
    public String resolve(HttpServletRequest request) {
        if (properties != null
                && properties.getProxy() != null
                && properties.getProxy().isTrustForwardedHeaders()) {
            String candidate = firstValidEntry(request.getHeader(properties.getProxy().getForwardedHeader()));
            if (candidate != null) {
                return candidate;
            }
        }
        return request.getRemoteAddr();
    }

    private String firstValidEntry(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String[] entries = header.split(",");
        for (String entry : entries) {
            String candidate = entry.trim();
            if (isIpAddress(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isIpAddress(String value) {
        if (value == null || value.isEmpty() || value.length() > 45) {
            return false;
        }
        return value.indexOf(':') >= 0 ? IPV6.matcher(value).matches() : IPV4.matcher(value).matches();
    }
}