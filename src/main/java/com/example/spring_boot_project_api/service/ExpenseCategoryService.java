package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.ExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseCategoryService {
    ExpenseCategoryResponse create(ExpenseCategoryRequest request);
    ExpenseCategoryResponse getByIdAndOwnerId(Long id, Long ownerId);
    List<ExpenseCategoryResponse> getAllByOwnerId(Long ownerId);
    Page<ExpenseCategoryResponse> getAllByOwnerId(Long ownerId, Pageable pageable);
    ExpenseCategoryResponse update(Long id, ExpenseCategoryRequest request);
    void delete(Long id, Long ownerId); 
}
