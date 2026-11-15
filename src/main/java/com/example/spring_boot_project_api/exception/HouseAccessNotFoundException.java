package com.example.spring_boot_project_api.exception;

public class HouseAccessNotFoundException extends RuntimeException {

    public HouseAccessNotFoundException(String message) {
        super(message);
    }

    public static HouseAccessNotFoundException forUserAndHouse(Long userId, Long houseId) {
        return new HouseAccessNotFoundException(
                "House access not found for user id " + userId + " on house id " + houseId);
    }
}
