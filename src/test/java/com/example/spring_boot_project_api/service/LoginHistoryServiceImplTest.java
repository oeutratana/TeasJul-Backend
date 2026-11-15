package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

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
import com.example.spring_boot_project_api.service.impl.LoginHistoryServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class LoginHistoryServiceImplTest {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36";

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Spy
    private LoginHistoryMapper loginHistoryMapper = new LoginHistoryMapper();

    @InjectMocks
    private LoginHistoryServiceImpl loginHistoryService;

    private User user(Long id) {
        return User.builder().id(id).username("johndoe").email("john@rental.com").build();
    }

    private LoginHistory entry(Long id, LoginOutcome outcome, LoginMethod method,
            LocalDateTime occurredAt) {
        return LoginHistory.builder()
                .id(id)
                .user(user(1L))
                .occurredAt(occurredAt)
                .outcome(outcome)
                .method(method)
                .ip("192.168.1.10")
                .userAgent(USER_AGENT)
                .build();
    }

    // ----- Recording -----

    @Test
    void recordShouldPersistOutcomeMethodIpAndUserAgent() {
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("192.168.1.10");

        loginHistoryService.record(user(1L), LoginOutcome.SUCCESS, LoginMethod.PASSWORD_2FA,
                httpServletRequest);

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());

        LoginHistory saved = captor.getValue();
        assertThat(saved.getOutcome()).isEqualTo(LoginOutcome.SUCCESS);
        assertThat(saved.getMethod()).isEqualTo(LoginMethod.PASSWORD_2FA);
        assertThat(saved.getIp()).isEqualTo("192.168.1.10");
        assertThat(saved.getUserAgent()).isEqualTo(USER_AGENT);
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
    }

    @Test
    void recordShouldReuseTheExistingClientIpResolver() {
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("203.0.113.7");

        loginHistoryService.record(user(1L), LoginOutcome.FAILED, LoginMethod.PASSWORD,
                httpServletRequest);

        // the project already owns this resolver; a second one is never created
        verify(clientIpResolver).resolve(httpServletRequest);
        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("203.0.113.7");
    }

    @Test
    void recordShouldNeverPersistSecretMaterial() {
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(USER_AGENT);
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("192.168.1.10");

        loginHistoryService.record(user(1L), LoginOutcome.SUCCESS, LoginMethod.PASSWORD_2FA,
                httpServletRequest);

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        LoginHistory saved = captor.getValue();

        // The persisted row carries only outcome, method, ip and user agent.
        // There is no field that could hold a password, an OTP, a reset or
        // challenge token, or an issued JWT.
        List<String> persistedValues = List.of(
                String.valueOf(saved.getOutcome()),
                String.valueOf(saved.getMethod()),
                String.valueOf(saved.getIp()),
                String.valueOf(saved.getUserAgent()));

        assertThat(persistedValues).noneMatch(v -> v.contains("Password123"));
        assertThat(persistedValues).noneMatch(v -> v.contains("123456"));
        assertThat(persistedValues).noneMatch(v -> v.toLowerCase().contains("eyj"));
        assertThat(persistedValues).noneMatch(v -> v.contains("Bearer"));
        assertThat(LoginHistory.class.getDeclaredFields())
                .noneMatch(f -> List.of("password", "passwordHash", "otp", "otpHash",
                        "token", "resetToken", "challengeToken", "jwt")
                        .contains(f.getName()));
    }

    @Test
    void recordShouldAllowUnknownUserForFailedAttempts() {
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(null);
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("198.51.100.4");

        loginHistoryService.record(null, LoginOutcome.FAILED, LoginMethod.PASSWORD,
                httpServletRequest);

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getOutcome()).isEqualTo(LoginOutcome.FAILED);
        assertThat(captor.getValue().getUserAgent()).isNull();
    }

    @Test
    void recordShouldTruncateAnOverlongUserAgent() {
        String huge = "x".repeat(1000);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(huge);
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("192.168.1.10");

        loginHistoryService.record(user(1L), LoginOutcome.SUCCESS, LoginMethod.PASSWORD,
                httpServletRequest);

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).hasSize(255);
    }

    // ----- Self-service ownership -----

    @Test
    void getMyHistoryShouldAlwaysUseTheAuthenticatedUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        Pageable pageable = PageRequest.of(0, 10);
        when(loginHistoryRepository.findByUserIdOrderByOccurredAtDesc(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(
                        entry(2L, LoginOutcome.SUCCESS, LoginMethod.PASSWORD,
                                LocalDateTime.of(2026, 9, 25, 22, 30)),
                        entry(1L, LoginOutcome.FAILED, LoginMethod.PASSWORD,
                                LocalDateTime.of(2026, 9, 25, 22, 25)))));

        PageResponse<LoginHistoryResponse> result = loginHistoryService.getMyHistory(pageable);

        // resolved from the security context, never from a request parameter
        verify(loginHistoryRepository).findByUserIdOrderByOccurredAtDesc(eq(7L), any());
        verify(loginHistoryRepository, never())
                .findByUserIdOrderByOccurredAtDesc(eq(1L), any());
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).outcome()).isEqualTo(LoginOutcome.SUCCESS);
        assertThat(result.content().get(0).ip()).isEqualTo("192.168.1.10");
        assertThat(result.content().get(1).outcome()).isEqualTo(LoginOutcome.FAILED);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    // ----- SUPER_ADMIN access -----

    @Test
    void superAdminShouldBeAbleToReadAnotherUsersHistory() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(loginHistoryRepository.findByUserIdOrderByOccurredAtDesc(eq(99L), any()))
                .thenReturn(new PageImpl<>(List.of(
                        entry(5L, LoginOutcome.SUCCESS, LoginMethod.PASSWORD_2FA,
                                LocalDateTime.of(2026, 9, 25, 22, 30)))));

        PageResponse<LoginHistoryResponse> result =
                loginHistoryService.getHistoryForUser(99L, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).method()).isEqualTo(LoginMethod.PASSWORD_2FA);
    }

    @Test
    void nonSuperAdminShouldBeDeniedAnotherUsersHistory() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() ->
                loginHistoryService.getHistoryForUser(99L, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to access this resource");

        verify(loginHistoryRepository, never())
                .findByUserIdOrderByOccurredAtDesc(anyLong(), any());
    }

    @Test
    void superAdminShouldGetNotFoundForAnUnknownAccount() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() ->
                loginHistoryService.getHistoryForUser(404L, PageRequest.of(0, 10)))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ----- Pagination -----

    @Test
    void getMyHistoryShouldMapTheRequestedPageOntoTheResponse() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 25, 22, 30);
        List<LoginHistory> rows = List.of(
                entry(6L, LoginOutcome.SUCCESS, LoginMethod.PASSWORD, base),
                entry(5L, LoginOutcome.SUCCESS, LoginMethod.PASSWORD, base.minusMinutes(5)),
                entry(4L, LoginOutcome.FAILED, LoginMethod.PASSWORD, base.minusMinutes(10)));

        PageRequest secondPage = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "occurredAt"));
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(loginHistoryRepository.findByUserIdOrderByOccurredAtDesc(eq(7L), any()))
                .thenReturn(new PageImpl<>(rows, secondPage, 5));

        PageResponse<LoginHistoryResponse> result = loginHistoryService.getMyHistory(secondPage);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).hasSize(3);
        assertThat(result.content()).extracting(LoginHistoryResponse::id)
                .containsExactly(6L, 5L, 4L);
    }

    @Test
    void getMyHistoryShouldReturnAnEmptyPageWhenThereIsNoHistory() {
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(loginHistoryRepository.findByUserIdOrderByOccurredAtDesc(eq(7L), any()))
                .thenReturn(Page.empty());

        PageResponse<LoginHistoryResponse> result =
                loginHistoryService.getMyHistory(PageRequest.of(0, 10));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isOne();
    }

    @Test
    void mapperShouldNotLeakTheUserEntity() {
        LoginHistoryResponse response = loginHistoryMapper.toResponse(
                entry(1L, LoginOutcome.SUCCESS, LoginMethod.PASSWORD, LocalDateTime.now()));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.outcome()).isEqualTo(LoginOutcome.SUCCESS);
        assertThat(response.method()).isEqualTo(LoginMethod.PASSWORD);
        assertThat(LoginHistoryResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactlyInAnyOrder("id", "occurredAt", "outcome", "method", "ip",
                        "userAgent");
    }
}
