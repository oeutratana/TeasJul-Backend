package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.mapper.ExpenseMapper;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ExpenseService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.TransactionScoped;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    // private final PaymentMethodRepository paymentMethodRepository;
    private final ExpenseMapper mapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_CREATE);
        Long ownerId = request.ownerId();
        requireOwnerScope(ownerId);

        // 1. Validate Owner
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found with ID: " + ownerId));

        // 2. Validate Duplicate Expense No for this Owner
        if (expenseRepository.existsByOwnerIdAndExpenseNo(ownerId, request.expenseNo())) {
            throw new IllegalArgumentException("Expense number already exists for this owner: " + request.expenseNo());
        }

        // 3. Enforce Cross-Owner Isolation for Category
        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Category ID " + request.categoryId() + " does not belong to owner ID " + ownerId));

        // 4. Enforce Cross-Owner Isolation for House (Optional)
        House house = null;
        if (request.houseId() != null) {
            house = houseRepository.findByIdAndOwnerId(request.houseId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("House ID " + request.houseId() + " does not belong to owner ID " + ownerId));
        }

        // 5. Enforce Cross-Owner Isolation for Payment Method (Optional)
        // PaymentMethod paymentMethod = null;
        // if (request.paymentMethodId() != null) {
        //     paymentMethod = paymentMethodRepository.findByIdAndOwnerId(request.paymentMethodId(), ownerId)
        //             .orElseThrow(() -> new IllegalArgumentException("Payment Method ID " + request.paymentMethodId() + " does not belong to owner ID " + ownerId));
        // }

        // 6. Creator is the authenticated user (client-supplied createdById is never trusted)
        User createdBy = securityUtils.getCurrentUser();

        Expense entity = mapper.toEntity(request, owner, house, category, createdBy);
        Expense savedEntity = expenseRepository.save(entity);
        return mapper.toResponse(savedEntity);
    }

    @Override
    public ExpenseResponse getByIdAndOwnerId(Long id, Long ownerId) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_VIEW);
        requireOwnerScope(ownerId);
        Expense entity = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));
        return mapper.toResponse(entity);
    }

    @Override
    public List<ExpenseResponse> getAllByOwnerId(Long ownerId) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_VIEW);
        requireOwnerScope(ownerId);
        return expenseRepository.findByOwnerId(ownerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<ExpenseResponse> getAllByOwnerId(Long ownerId, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_VIEW);
        requireOwnerScope(ownerId);
        return expenseRepository.findByOwnerId(ownerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_UPDATE);
        Long ownerId = request.ownerId();
        requireOwnerScope(ownerId);

        Expense existingExpense = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));

        if (expenseRepository.existsByOwnerIdAndExpenseNoAndIdNot(ownerId, request.expenseNo(), id)) {
            throw new IllegalArgumentException("Expense number already exists for this owner: " + request.expenseNo());
        }

        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Category ID " + request.categoryId() + " does not belong to owner ID " + ownerId));

        House house = null;
        if (request.houseId() != null) {
            house = houseRepository.findByIdAndOwnerId(request.houseId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("House ID " + request.houseId() + " does not belong to owner ID " + ownerId));
        }

        // PaymentMethod paymentMethod = null;
        // if (request.paymentMethodId() != null) {
        //     paymentMethod = paymentMethodRepository.findByIdAndOwnerId(request.paymentMethodId(), ownerId)
        //             .orElseThrow(() -> new IllegalArgumentException("Payment Method ID " + request.paymentMethodId() + " does not belong to owner ID " + ownerId));
        // }

        existingExpense.setExpenseNo(request.expenseNo());
        existingExpense.setHouse(house);
        existingExpense.setCategory(category);
        existingExpense.setDescription(request.description());
        existingExpense.setAmount(request.amount());
        existingExpense.setExpenseDate(request.expenseDate());
        // existingExpense.setPaymentMethod(paymentMethod);
        if (request.status() != null) {
            existingExpense.setStatus(request.status());
        }

        Expense updatedEntity = expenseRepository.save(existingExpense);
        return mapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        securityUtils.requirePermission(PermissionConstants.EXPENSE_DELETE);
        requireOwnerScope(ownerId);
        Expense expense = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));
        expenseRepository.delete(expense);
    }

    /**
     * Ensures the current user may act on resources owned by {@code ownerId}:
     * SUPER_ADMIN may act on any owner, an OWNER only on their own resources, and
     * a MANAGER only on owners reachable through their managed houses. The
     * client-supplied {@code ownerId} is validated against this scope, never
     * trusted as proof of authorization.
     */
    private void requireOwnerScope(Long ownerId) {
        if (ownerId == null) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return;
        }
        if (ownerId.equals(securityUtils.getCurrentUserId())
                || houseAccessService.getScopedOwnerIds().contains(ownerId)) {
            return;
        }
        throw new AccessDeniedException(ACCESS_DENIED_MSG);
    }
}
