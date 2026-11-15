package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceAssignmentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceAssignmentResponse;
import com.example.spring_boot_project_api.exception.InvalidMaintenanceOperationException;
import com.example.spring_boot_project_api.exception.MaintenanceAssignmentNotFoundException;
import com.example.spring_boot_project_api.exception.MaintenanceRequestNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.MaintenanceAssignmentMapper;
import com.example.spring_boot_project_api.model.MaintenanceAssignment;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.MaintenanceAssignmentRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.MaintenanceAssignmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintenanceAssignmentServiceImpl implements MaintenanceAssignmentService {

    private final MaintenanceAssignmentRepository assignmentRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final MaintenanceAssignmentMapper assignmentMapper;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public MaintenanceAssignmentResponse createAssignment(Long requestId,
            CreateMaintenanceAssignmentRequest request) {
        User owner = getCurrentOwner();
        MaintenanceRequest maintenanceRequest = getOwnedRequest(requestId, owner.getId());
        User assignee = userRepository.findById(request.assignedTo())
                .orElseThrow(() -> UserNotFoundException.forId(request.assignedTo()));

        ensureAssigneeCanWorkOnHouse(assignee, maintenanceRequest);

        MaintenanceAssignment assignment = assignmentMapper.toEntity(request);
        assignment.setOwner(owner);
        assignment.setRequest(maintenanceRequest);
        assignment.setAssignedTo(assignee);
        assignment = assignmentRepository.save(assignment);

        if (maintenanceRequest.getStatus() == MaintenanceRequestStatus.PENDING) {
            maintenanceRequest.setStatus(MaintenanceRequestStatus.IN_PROGRESS);
            requestRepository.save(maintenanceRequest);
        }

        return assignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceAssignmentResponse> getAssignments(Long requestId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        return assignmentRepository.findAllByRequestIdOrderByAssignedAtAsc(requestId).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void completeAssignment(Long requestId, Long assignmentId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        MaintenanceAssignment assignment = assignmentRepository.findByIdAndRequestId(assignmentId, requestId)
                .orElseThrow(() -> MaintenanceAssignmentNotFoundException.forId(assignmentId));

        if (assignment.getCompletedAt() != null) {
            throw new InvalidMaintenanceOperationException(
                    "Maintenance assignment " + assignmentId + " is already completed");
        }

        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    private void ensureAssigneeCanWorkOnHouse(User assignee, MaintenanceRequest maintenanceRequest) {
        Long ownerId = maintenanceRequest.getOwner().getId();
        Long houseId = maintenanceRequest.getHouse().getId();

        boolean isOwner = assignee.getId().equals(ownerId);
        boolean hasHouseAccess =
                userHouseAccessRepository.existsByUserIdAndHouseId(assignee.getId(), houseId);

        if (!isOwner && !hasHouseAccess) {
            throw new AccessDeniedException(
                    "User " + assignee.getUsername() + " does not have access to house " + houseId);
        }
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
