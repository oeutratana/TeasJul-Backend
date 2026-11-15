package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.enums.ExpenseStatus;
import com.example.spring_boot_project_api.mapper.ExpenseMapper;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Object-level authorization regression tests for the expense API (finding F-02).
 * The client-supplied {@code ownerId} must never grant cross-owner access.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseServiceImplAuthzTest {

    private static final long CURRENT_USER = 1L;
    private static final long OTHER_OWNER = 2L;

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;
    @Mock
    private ExpenseMapper mapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;

    @InjectMocks
    private ExpenseServiceImpl service;

    private void scopedToCurrentUserOnly() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(CURRENT_USER);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(CURRENT_USER));
    }

    private ExpenseRequest request(Long ownerId, Long createdById) {
        return new ExpenseRequest(ownerId, "E-1", null, 3L, "desc",
                BigDecimal.TEN, LocalDate.now(), null, createdById);
    }

    private ExpenseResponse sampleResponse() {
        return new ExpenseResponse(5L, CURRENT_USER, "E-1", null, null, 3L, null,
                "desc", BigDecimal.TEN, LocalDate.now(), ExpenseStatus.PAID,
                CURRENT_USER, "user", null, null);
    }

    // ---------- read ----------

    @Test
    void getById_ownerCannotReadOtherOwnersExpense() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getByIdAndOwnerId(5L, OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).findByIdAndOwnerId(anyLong(), anyLong());
    }

    @Test
    void getById_ownerReadsOwnExpense() {
        scopedToCurrentUserOnly();
        Expense expense = new Expense();
        when(expenseRepository.findByIdAndOwnerId(5L, CURRENT_USER)).thenReturn(Optional.of(expense));
        when(mapper.toResponse(expense)).thenReturn(sampleResponse());

        assertThat(service.getByIdAndOwnerId(5L, CURRENT_USER)).isNotNull();
    }

    @Test
    void getById_superAdminCanReadOtherOwnersExpense() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        Expense expense = new Expense();
        when(expenseRepository.findByIdAndOwnerId(5L, OTHER_OWNER)).thenReturn(Optional.of(expense));
        when(mapper.toResponse(expense)).thenReturn(sampleResponse());

        assertThat(service.getByIdAndOwnerId(5L, OTHER_OWNER)).isNotNull();
    }

    @Test
    void list_ownerCannotListOtherOwnersExpenses() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getAllByOwnerId(OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).findByOwnerId(anyLong());
    }

    @Test
    void pagedList_ownerCannotListOtherOwnersExpenses() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getAllByOwnerId(OTHER_OWNER, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).findByOwnerId(anyLong(), any(Pageable.class));
    }

    // ---------- write ----------

    @Test
    void update_ownerCannotUpdateOtherOwnersExpense() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.update(5L, request(OTHER_OWNER, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).findByIdAndOwnerId(anyLong(), anyLong());
    }

    @Test
    void delete_ownerCannotDeleteOtherOwnersExpense() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.delete(5L, OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void create_ownerCannotCreateExpenseForAnotherOwner() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.create(request(OTHER_OWNER, OTHER_OWNER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void create_setsCreatedByToCurrentUserAndIgnoresClientValue() {
        scopedToCurrentUserOnly();
        User currentUser = new User();
        currentUser.setId(CURRENT_USER);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        User owner = new User();
        owner.setId(CURRENT_USER);
        when(userRepository.findById(CURRENT_USER)).thenReturn(Optional.of(owner));

        ExpenseCategory category = new ExpenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(3L, CURRENT_USER))
                .thenReturn(Optional.of(category));

        Expense entity = new Expense();
        when(mapper.toEntity(any(ExpenseRequest.class), eq(owner), isNull(), eq(category), eq(currentUser)))
                .thenReturn(entity);
        when(expenseRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());

        service.create(request(CURRENT_USER, 999L));

        ArgumentCaptor<User> createdBy = ArgumentCaptor.forClass(User.class);
        verify(mapper).toEntity(any(ExpenseRequest.class), eq(owner), isNull(), eq(category),
                createdBy.capture());
        assertThat(createdBy.getValue()).isSameAs(currentUser);
    }

    // ---------- manager scope ----------

    @Test
    void getById_managerCanReadExpenseOfManagerManagedOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(CURRENT_USER);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(OTHER_OWNER));
        Expense expense = new Expense();
        when(expenseRepository.findByIdAndOwnerId(5L, OTHER_OWNER)).thenReturn(Optional.of(expense));
        when(mapper.toResponse(expense)).thenReturn(sampleResponse());

        assertThat(service.getByIdAndOwnerId(5L, OTHER_OWNER)).isNotNull();
    }

    @Test
    void getById_managerCannotReadExpenseOfUnmanagedOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(CURRENT_USER);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(CURRENT_USER));

        assertThatThrownBy(() -> service.getByIdAndOwnerId(5L, OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
    }
}
