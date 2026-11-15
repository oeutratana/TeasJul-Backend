package com.example.spring_boot_project_api.exception;

public class OwnerSettingNotFoundException extends RuntimeException {

    public OwnerSettingNotFoundException(String message) {
        super(message);
    }

    public static OwnerSettingNotFoundException forOwner(Long ownerId) {
        return new OwnerSettingNotFoundException("Settings not found for owner: " + ownerId);
    }
}
