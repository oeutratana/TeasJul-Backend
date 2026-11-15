package com.example.spring_boot_project_api.mapper;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.FeedbackResponse;
import com.example.spring_boot_project_api.model.Feedback;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedbackMapper {

    private final UserRepository userRepository;

    public FeedbackResponse toDto(Feedback feedback) {
        String ownerName = null;
        if (feedback.getOwnerId() != null) {
            ownerName = userRepository.findById(feedback.getOwnerId())
                    .map(this::toOwnerName)
                    .orElse(null);
        }
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getOwnerId(),
                ownerName,
                feedback.getRating(),
                feedback.getComment(),
                feedback.getStatus(),
                feedback.getCreatedAt(),
                feedback.getReviewedAt());
    }

    private String toOwnerName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return name.isBlank() ? null : name;
    }
}