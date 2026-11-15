package com.example.spring_boot_project_api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.spring_boot_project_api.dto.response.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void roomNotFoundReturns404InsteadOf500() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/contract_tenant");

        ResponseEntity<ErrorResponse> response = handler.handleRoomNotFound(
                RoomNotFoundException.forId(3L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Room not found with id: 3");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/contract_tenant");
    }
}