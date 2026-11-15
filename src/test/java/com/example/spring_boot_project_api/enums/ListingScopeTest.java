package com.example.spring_boot_project_api.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ListingScopeTest {

    @Test
    void exposesAllRequiredListingScopes() {
        assertThat(ListingScope.values())
            .containsExactly(
                ListingScope.INDIVIDUAL_ROOM,
                ListingScope.MULTIPLE_ROOMS,
                ListingScope.ENTIRE_PROPERTY);
    }

    @Test
    void canBeStoredAsString() {
        assertThat(ListingScope.MULTIPLE_ROOMS.name()).isEqualTo("MULTIPLE_ROOMS");
    }
}