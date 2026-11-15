package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.model.LoginHistory;

@Component
public class LoginHistoryMapper {

    public LoginHistoryResponse toResponse(LoginHistory loginHistory) {
        return new LoginHistoryResponse(
                loginHistory.getId(),
                loginHistory.getOccurredAt(),
                loginHistory.getOutcome(),
                loginHistory.getMethod(),
                loginHistory.getIp(),
                loginHistory.getUserAgent());
    }
}
