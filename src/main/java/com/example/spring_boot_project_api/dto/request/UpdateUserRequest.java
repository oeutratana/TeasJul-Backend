package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 30)
        String phone,

        @Size(max = 500)
        String avatar
) {
}
