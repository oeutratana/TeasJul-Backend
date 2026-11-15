package com.example.spring_boot_project_api.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void internalRolesAreAllNonTenantRoles() {
        assertThat(EmailValidator.isInternalRole("OWNER")).isTrue();
        assertThat(EmailValidator.isInternalRole("MANAGER")).isTrue();
        assertThat(EmailValidator.isInternalRole("ADMIN")).isTrue();
        assertThat(EmailValidator.isInternalRole("STAFF")).isTrue();
    }

    @Test
    void tenantRoleIsNotInternal() {
        assertThat(EmailValidator.isInternalRole("TENANT")).isFalse();
        assertThat(EmailValidator.isInternalRole("tenant")).isFalse();
    }

    @Test
    void internalDomainMatchesRentalCom() {
        assertThat(EmailValidator.isOnInternalDomain("manager@rental.com")).isTrue();
        assertThat(EmailValidator.isOnInternalDomain("MANAGER@RENTAL.COM")).isTrue();
    }

    @Test
    void externalDomainsAreNotInternal() {
        assertThat(EmailValidator.isOnInternalDomain("manager@gmail.com")).isFalse();
        assertThat(EmailValidator.isOnInternalDomain("manager@yahoo.com")).isFalse();
        assertThat(EmailValidator.isOnInternalDomain("manager@rental.com.evil.com")).isFalse();
    }

    @Test
    void invalidEmailsAreNotInternal() {
        assertThat(EmailValidator.isOnInternalDomain(null)).isFalse();
        assertThat(EmailValidator.isOnInternalDomain("")).isFalse();
        assertThat(EmailValidator.isOnInternalDomain("not-an-email")).isFalse();
        assertThat(EmailValidator.isOnInternalDomain("manager@")).isFalse();
    }

    @Test
    void requirementMessageMentionsDomain() {
        assertThat(EmailValidator.internalEmailRequirement())
                .contains("@rental.com");
    }
}