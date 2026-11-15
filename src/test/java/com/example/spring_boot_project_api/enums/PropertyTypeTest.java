package com.example.spring_boot_project_api.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PropertyTypeTest {

    @Test
    void exposesAllRequiredPropertyTypes() {
        assertThat(PropertyType.values())
            .containsExactly(
                PropertyType.ROOM,
                PropertyType.HOUSE,
                PropertyType.VILLA,
                PropertyType.APARTMENT,
                PropertyType.CONDO,
                PropertyType.OTHER);
    }

    @Test
    void canBeStoredAsString() {
        assertThat(PropertyType.VILLA.name()).isEqualTo("VILLA");
    }
}