package com.example.spring_boot_project_api.exception;

public class UserRoleNotFoundException extends RuntimeException {

    public UserRoleNotFoundException(String message) {
        super(message);
    }

    public static UserRoleNotFoundException forIds(Long userId, Long roleId) {
        return new UserRoleNotFoundException(
                "Role " + roleId + " is not assigned to user " + userId);
    }
}
