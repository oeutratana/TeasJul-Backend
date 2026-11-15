package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;

public interface ExpenseService {
    ExpenseResponse create(ExpenseRequest request);
    ExpenseResponse getByIdAndOwnerId(Long id, Long ownerId);
    List<ExpenseResponse> getAllByOwnerId(Long ownerId);
    Page<ExpenseResponse> getAllByOwnerId(Long ownerId, Pageable pageable);
    ExpenseResponse update(Long id, ExpenseRequest request);
    void delete(Long id, Long ownerId);
}
