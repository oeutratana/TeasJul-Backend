package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCommentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCommentResponse;

public interface MaintenanceCommentService {

    MaintenanceCommentResponse createComment(Long requestId, CreateMaintenanceCommentRequest request);

    List<MaintenanceCommentResponse> getComments(Long requestId);

    void deleteComment(Long requestId, Long commentId);
}
