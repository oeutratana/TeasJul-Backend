package com.example.spring_boot_project_api.enums;

/**
 * Result of a sign-in attempt. Only the two outcomes the existing
 * authentication flow can produce are modelled; no additional states are
 * invented.
 */
public enum LoginOutcome {
    SUCCESS,
    FAILED
}
