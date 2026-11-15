package com.example.spring_boot_project_api.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoomPublicationStatusTest {

    @Test
    void exposesAllRequiredWorkflowStates() {
        assertThat(RoomPublicationStatus.values())
            .containsExactly(
                RoomPublicationStatus.DRAFT,
                RoomPublicationStatus.PENDING_APPROVAL,
                RoomPublicationStatus.PUBLISHED,
                RoomPublicationStatus.REJECTED,
                RoomPublicationStatus.UNPUBLISHED);
    }

    @Test
    void isSeparateFromRoomStatus() {
        assertThat(RoomStatus.values())
            .containsExactly(
                RoomStatus.AVAILABLE,
                RoomStatus.BOOKED,
                RoomStatus.OCCUPIED,
                RoomStatus.MAINTENANCE,
                RoomStatus.RESERVED);
    }
}