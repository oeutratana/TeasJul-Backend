package com.example.spring_boot_project_api.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

class RoomBookingRequestValidationTest {

    @Test
    void requiresCustomerNamePhoneAndExpectedMoveInDate() {
        RoomBookingRequest request = new RoomBookingRequest();
        request.setRoomId(1L);

        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(request);
        var messages = violations.stream().map(v -> v.getMessage()).toList();

        assertThat(messages).contains("Customer name is required", "Customer phone is required", "Expected move-in date is required");
    }

    @Test
    void rejectsExpectedMoveInDateInThePast() {
        RoomBookingRequest request = new RoomBookingRequest();
        request.setRoomId(1L);
        request.setCustomerName("Jane");
        request.setCustomerPhone("012345678");
        request.setExpectedMoveInDate(LocalDate.now().minusDays(1));

        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request))
                .anyMatch(v -> v.getMessage().equals("Expected move-in date cannot be in the past"));
    }
}
