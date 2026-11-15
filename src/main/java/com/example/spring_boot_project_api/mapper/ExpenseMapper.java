package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.enums.ExpenseStatus;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;

@Component
public class ExpenseMapper {
    public Expense toEntity(ExpenseRequest request, User owner, House house, ExpenseCategory category, User createdBy) {
        return Expense.builder()
                .owner(owner)
                .expenseNo(request.expenseNo())
                .house(house)
                .category(category)
                .description(request.description())
                .amount(request.amount())
                .expenseDate(request.expenseDate())
                // .paymentMethod(paymentMethod)
                .status(request.status() != null ? request.status() : ExpenseStatus.PAID)
                .createdBy(createdBy)
                .build();
    }

    public ExpenseResponse toResponse(Expense entity) {
        return new ExpenseResponse(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getExpenseNo(),
                entity.getHouse() != null ? entity.getHouse().getId() : null,
                entity.getHouse() != null ? entity.getHouse().getName() : null,
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getExpenseDate(),
                // entity.getPaymentMethod() != null ? entity.getPaymentMethod().getId() : null,
                // entity.getPaymentMethod() != null ? entity.getPaymentMethod().getName() : null,
                entity.getStatus(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null,
                entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
