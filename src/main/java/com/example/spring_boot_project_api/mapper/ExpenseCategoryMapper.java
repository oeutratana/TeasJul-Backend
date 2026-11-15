package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.ExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.enums.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.User;

@Component
public class ExpenseCategoryMapper {
    public ExpenseCategory toEntity(ExpenseCategoryRequest request, User owner) {
        return ExpenseCategory.builder()
                .owner(owner)
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .status(request.status() != null ? request.status() : ExpenseCategoryStatus.ACTIVE)
                .build();
    }

    public ExpenseCategoryResponse toResponse(ExpenseCategory entity) {
        return new ExpenseCategoryResponse(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
