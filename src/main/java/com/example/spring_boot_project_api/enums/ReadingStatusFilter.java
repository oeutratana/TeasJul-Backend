package com.example.spring_boot_project_api.enums;

/**
 * Reading-status filter for the monthly meter-reading sheet.
 *
 * <p>A room counts as {@code ENTERED} when every ACTIVE meter of the room has a
 * current reading persisted inside the selected billing month. The filter is
 * evaluated against persisted readings on the server, never against frontend
 * input state.</p>
 */
public enum ReadingStatusFilter {
    ALL,
    ENTERED,
    NOT_ENTERED
}