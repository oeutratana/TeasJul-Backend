package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCommentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCommentResponse;
import com.example.spring_boot_project_api.exception.MaintenanceCommentNotFoundException;
import com.example.spring_boot_project_api.exception.MaintenanceRequestNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.MaintenanceCommentMapper;
import com.example.spring_boot_project_api.model.MaintenanceComment;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.MaintenanceCommentRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.MaintenanceCommentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintenanceCommentServiceImpl implements MaintenanceCommentService {

    private final MaintenanceCommentRepository commentRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final MaintenanceCommentMapper commentMapper;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public MaintenanceCommentResponse createComment(Long requestId, CreateMaintenanceCommentRequest request) {
        User owner = getCurrentOwner();
        MaintenanceRequest maintenanceRequest = getOwnedRequest(requestId, owner.getId());

        MaintenanceComment comment = commentMapper.toEntity(request);
        comment.setOwner(owner);
        comment.setRequest(maintenanceRequest);
        comment.setUser(owner);

        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceCommentResponse> getComments(Long requestId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        return commentRepository.findAllByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteComment(Long requestId, Long commentId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        MaintenanceComment comment = commentRepository.findByIdAndRequestId(commentId, requestId)
                .orElseThrow(() -> MaintenanceCommentNotFoundException.forId(commentId));

        commentRepository.delete(comment);
    }

    private MaintenanceRequest getOwnedRequest(Long requestId, Long ownerId) {
        MaintenanceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> MaintenanceRequestNotFoundException.forId(requestId));
        if (req.getOwner().getId().equals(ownerId)) {
            return req;
        }
        if (houseAccessService.canAccessHouse(req.getHouse().getId())) {
            return req;
        }
        throw MaintenanceRequestNotFoundException.forId(requestId);
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
