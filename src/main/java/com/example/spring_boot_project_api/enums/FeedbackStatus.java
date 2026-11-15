package com.example.spring_boot_project_api.enums;

/**
 * Publication status for owner feedback about the TeasJul system.
 *
 * <p>Feedback is submitted by a property OWNER about the TeasJul platform
 * itself -- it is NOT a rating of an owner, property, house, room or room
 * type. A submission always starts as {@code PENDING} and is only shown on
 * the public website once SUPER_ADMIN publishes it. {@code UNPUBLISHED}
 * feedback never appears publicly.</p>
 */
public enum FeedbackStatus {
    PENDING,
    PUBLISHED,
    UNPUBLISHED
}