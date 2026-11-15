package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCommentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCommentResponse;
import com.example.spring_boot_project_api.model.MaintenanceComment;

@Component
public class MaintenanceCommentMapper {

    public MaintenanceCommentResponse toResponse(MaintenanceComment comment) {
        return new MaintenanceCommentResponse(
                comment.getId(),
                comment.getRequest().getId(),
                comment.getUser() != null ? comment.getUser().getId() : null,
                comment.getComment(),
                comment.getCreatedAt());
    }

    public MaintenanceComment toEntity(CreateMaintenanceCommentRequest request) {
        return MaintenanceComment.builder()
                .comment(request.comment())
                .build();
    }
}
