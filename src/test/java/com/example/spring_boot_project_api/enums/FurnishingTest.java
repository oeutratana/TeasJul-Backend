package com.example.spring_boot_project_api.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FurnishingTest {

    @Test
    void exposesAllRequiredFurnishingStates() {
        assertThat(Furnishing.values())
            .containsExactly(
                Furnishing.FULLY_FURNISHED,
                Furnishing.SEMI_FURNISHED,
                Furnishing.UNFURNISHED);
    }

    @Test
    void canBeStoredAsString() {
        assertThat(Furnishing.UNFURNISHED.name()).isEqualTo("UNFURNISHED");
    }
}