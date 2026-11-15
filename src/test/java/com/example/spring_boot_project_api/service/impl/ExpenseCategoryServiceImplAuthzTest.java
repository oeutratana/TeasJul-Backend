package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.ExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.enums.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.mapper.ExpenseCategoryMapper;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Authorization regression tests for the expense-category API (finding F-03).
 * Every operation must enforce both a function-level permission and owner scope,
 * never trusting the client-supplied ownerId.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseCategoryServiceImplAuthzTest {

    private static final long CURRENT_USER = 1L;
    private static final long OTHER_OWNER = 2L;

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseCategoryMapper mapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;

    @InjectMocks
    private ExpenseCategoryServiceImpl service;

    private void scopedToCurrentUserOnly() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(CURRENT_USER);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(CURRENT_USER));
    }

    private ExpenseCategoryRequest request(Long ownerId) {
        return new ExpenseCategoryRequest(ownerId, "Supplies", "SUP", "desc",
                ExpenseCategoryStatus.ACTIVE);
    }

    private ExpenseCategoryResponse sampleResponse(Long ownerId) {
        return new ExpenseCategoryResponse(5L, ownerId, "Supplies", "SUP", "desc",
                ExpenseCategoryStatus.ACTIVE, null, null);
    }

    // ---------- function-level permission ----------

    @Test
    void getById_unprivilegedUserIsDeniedByPermissionCheck() {
        doThrow(new AccessDeniedException("denied"))
                .when(securityUtils).requirePermission(PermissionConstants.EXPENSE_VIEW);

        assertThatThrownBy(() -> service.getByIdAndOwnerId(5L, CURRENT_USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_enforcesCreatePermission() {
        scopedToCurrentUserOnly();
        User owner = new User();
        owner.setId(CURRENT_USER);
        when(userRepository.findById(CURRENT_USER)).thenReturn(Optional.of(owner));
        stubCategoryPersistence();
        when(mapper.toResponse(any(ExpenseCategory.class))).thenReturn(sampleResponse(CURRENT_USER));

        service.create(request(CURRENT_USER));

        verify(securityUtils).requirePermission(PermissionConstants.EXPENSE_CREATE);
    }

    private void stubCategoryPersistence() {
        when(expenseCategoryRepository.existsByOwnerIdAndCode(any(), any())).thenReturn(false);
        when(expenseCategoryRepository.existsByOwnerIdAndName(any(), any())).thenReturn(false);
        when(mapper.toEntity(any(ExpenseCategoryRequest.class), any(User.class)))
                .thenReturn(new ExpenseCategory());
        when(expenseCategoryRepository.save(any(ExpenseCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- owner scope ----------

    @Test
    void getById_ownerCannotReadOtherOwnersCategory() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getByIdAndOwnerId(5L, OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).findByIdAndOwnerId(anyLong(), anyLong());
    }

    @Test
    void list_ownerCannotListOtherOwnersCategories() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getAllByOwnerId(OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).findByOwnerId(anyLong());
    }

    @Test
    void pagedList_ownerCannotListOtherOwnersCategories() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.getAllByOwnerId(OTHER_OWNER, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).findByOwnerId(anyLong(), any(Pageable.class));
    }

    @Test
    void create_ownerCannotCreateCategoryForAnotherOwner() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.create(request(OTHER_OWNER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).save(any(ExpenseCategory.class));
    }

    @Test
    void update_ownerChangingOwnerIdDoesNotBypassScope() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.update(5L, request(OTHER_OWNER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).findByIdAndOwnerId(anyLong(), anyLong());
    }

    @Test
    void delete_ownerCannotDeleteOtherOwnersCategory() {
        scopedToCurrentUserOnly();

        assertThatThrownBy(() -> service.delete(5L, OTHER_OWNER))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseCategoryRepository, never()).delete(any(ExpenseCategory.class));
    }

    // ---------- authorized paths ----------

    @Test
    void getById_ownerReadsOwnCategory() {
        scopedToCurrentUserOnly();
        ExpenseCategory category = new ExpenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(5L, CURRENT_USER))
                .thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(sampleResponse(CURRENT_USER));

        assertThat(service.getByIdAndOwnerId(5L, CURRENT_USER)).isNotNull();
    }

    @Test
    void getById_superAdminCanReadAnyOwnersCategory() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        ExpenseCategory category = new ExpenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(5L, OTHER_OWNER))
                .thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(sampleResponse(OTHER_OWNER));

        assertThat(service.getByIdAndOwnerId(5L, OTHER_OWNER)).isNotNull();
    }

    @Test
    void create_superAdminCanCreateForAnyOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        User owner = new User();
        owner.setId(OTHER_OWNER);
        when(userRepository.findById(OTHER_OWNER)).thenReturn(Optional.of(owner));
        stubCategoryPersistence();
        when(mapper.toResponse(any(ExpenseCategory.class))).thenReturn(sampleResponse(OTHER_OWNER));

        assertThat(service.create(request(OTHER_OWNER))).isNotNull();
    }
}
