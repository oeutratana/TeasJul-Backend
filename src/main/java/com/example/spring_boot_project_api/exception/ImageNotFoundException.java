package com.example.spring_boot_project_api.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(String message) {
        super(message);
    }

    public static ImageNotFoundException forId(Long id) {
        return new ImageNotFoundException("Image not found with id: " + id);
    }
}