package com.example.spring_boot_project_api.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.enums.NearbyLocationCategory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class NearbyLocationRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validCreateRequestPasses() {
        CreateNearbyLocationRequest request = new CreateNearbyLocationRequest(
                "Central Market", NearbyLocationCategory.MARKET,
                new BigDecimal("0.80"), 6);

        Set<ConstraintViolation<CreateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void createRequestRejectsMissingName() {
        CreateNearbyLocationRequest request = new CreateNearbyLocationRequest(
                " ", NearbyLocationCategory.MARKET, new BigDecimal("0.80"), 6);

        Set<ConstraintViolation<CreateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void createRequestRejectsMissingCategory() {
        CreateNearbyLocationRequest request = new CreateNearbyLocationRequest(
                "Central Market", null, new BigDecimal("0.80"), 6);

        Set<ConstraintViolation<CreateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("category"));
    }

    @Test
    void createRequestRejectsNegativeDistance() {
        CreateNearbyLocationRequest request = new CreateNearbyLocationRequest(
                "Central Market", NearbyLocationCategory.MARKET,
                new BigDecimal("-1.00"), 6);

        Set<ConstraintViolation<CreateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("distanceKm"));
    }

    @Test
    void createRequestRejectsNegativeTravelMinutes() {
        CreateNearbyLocationRequest request = new CreateNearbyLocationRequest(
                "Central Market", NearbyLocationCategory.MARKET,
                new BigDecimal("0.80"), -1);

        Set<ConstraintViolation<CreateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("travelMinutes"));
    }

    @Test
    void updateRequestRejectsNegativeDistance() {
        UpdateNearbyLocationRequest request = new UpdateNearbyLocationRequest(
                "Market", NearbyLocationCategory.MARKET,
                new BigDecimal("-2.00"), null);

        Set<ConstraintViolation<UpdateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("distanceKm"));
    }

    @Test
    void updateRequestAllNullPasses() {
        UpdateNearbyLocationRequest request = new UpdateNearbyLocationRequest(null, null, null, null);

        Set<ConstraintViolation<UpdateNearbyLocationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }
}