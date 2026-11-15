package com.example.spring_boot_project_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.ratelimit.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

/**
 * Structured security/audit logger. Emits a single log line per event with
 * useful metadata (user id, event type, IP, user agent). Never logs secrets:
 * passwords, OTP values, JWT tokens, reset tokens, SMTP credentials or API keys.
 */
@Component
@RequiredArgsConstructor
public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final ClientIpResolver clientIpResolver;

    public void log(Long userId, String event, HttpServletRequest request, String detail) {
        String ip = request == null ? "-" : safe(clientIpResolver.resolve(request));
        String ua = request == null ? "-" : safe(request.getHeader("User-Agent"));
        log.info("user={} event={} ip={} user_agent={} detail={}",
                userId == null ? "-" : userId, event, ip, ua, safe(detail));
    }

    public void log(Long userId, String event, String detail) {
        log(userId, event, null, detail);
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }
}