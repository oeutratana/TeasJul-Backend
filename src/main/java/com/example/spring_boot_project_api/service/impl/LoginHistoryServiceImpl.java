package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.LoginHistoryMapper;
import com.example.spring_boot_project_api.model.LoginHistory;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.ratelimit.ClientIpResolver;
import com.example.spring_boot_project_api.repository.LoginHistoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.LoginHistoryService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Persistent login history.
 *
 * <p>Access rules (resolved server-side, never trusted from the client):</p>
 * <ul>
 *   <li>{@link #getMyHistory} always reads the row set of the user resolved from
 *       the Spring Security context, so a caller can never reach another
 *       account's history through the self-service endpoint.</li>
 *   <li>{@link #getHistoryForUser} requires the SUPER_ADMIN role, enforced with
 *       an explicit role check like the existing owner administration, never the
 *       generic permission check (OWNER is implicitly granted every permission
 *       and must not gain visibility into other people's sign-ins).</li>
 * </ul>
 *
 * <p>Only the outcome, the authentication path, the resolved client IP and the
 * raw {@code User-Agent} header are stored. Passwords, OTP codes, reset and
 * challenge tokens and issued JWTs are never written here.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    /** {@code user_agent} is VARCHAR(255); longer headers are truncated. */
    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final LoginHistoryMapper loginHistoryMapper;
    private final SecurityUtils securityUtils;
    private final ClientIpResolver clientIpResolver;

    /**
     * Runs in its own transaction on purpose: the login flow is transactional
     * and throws right after recording a failure, which would otherwise roll the
     * {@link LoginOutcome#FAILED} row back and lose the audit trail.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, LoginOutcome outcome, LoginMethod method, HttpServletRequest request) {
        loginHistoryRepository.save(LoginHistory.builder()
                .user(user)
                .occurredAt(LocalDateTime.now())
                .outcome(outcome)
                .method(method)
                .ip(request == null ? null : truncate(clientIpResolver.resolve(request), 45))
                .userAgent(request == null ? null : truncate(request.getHeader("User-Agent"),
                        MAX_USER_AGENT_LENGTH))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> getMyHistory(Pageable pageable) {
        return toPageResponse(loginHistoryRepository
                .findByUserIdOrderByOccurredAtDesc(securityUtils.getCurrentUserId(), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> getHistoryForUser(Long userId, Pageable pageable) {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException.forId(userId);
        }
        return toPageResponse(
                loginHistoryRepository.findByUserIdOrderByOccurredAtDesc(userId, pageable));
    }

    private PageResponse<LoginHistoryResponse> toPageResponse(Page<LoginHistory> page) {
        return PageResponse.of(
                page.getContent().stream().map(loginHistoryMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
