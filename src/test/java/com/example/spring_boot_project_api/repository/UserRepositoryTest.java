package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(user);
    }

    @Test
    void findByUsernameShouldReturnUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        persistUser("johndoe_" + suffix, "john_" + suffix + "@example.com");

        assertThat(userRepository.findByUsername("johndoe_" + suffix))
                .hasValueSatisfying(user -> {
                    assertThat(user.getEmail()).isEqualTo("john_" + suffix + "@example.com");
                    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void findByEmailShouldReturnUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        persistUser("janedoe_" + suffix, "jane_" + suffix + "@example.com");

        assertThat(userRepository.findByEmail("jane_" + suffix + "@example.com"))
                .hasValueSatisfying(user -> assertThat(user.getUsername()).isEqualTo("janedoe_" + suffix));
    }

    @Test
    void findByUnknownUsernameShouldReturnEmpty() {
        assertThat(userRepository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void existsByUsernameAndEmailShouldReflectPersistedUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        persistUser("unique_user_" + suffix, "unique_" + suffix + "@example.com");

        assertThat(userRepository.existsByUsername("unique_user_" + suffix)).isTrue();
        assertThat(userRepository.existsByEmail("unique_" + suffix + "@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("missing")).isFalse();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void findAvailableForTenantSearchesAndExcludesLinkedUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User owner = persistUser("owner_" + suffix, "owner_" + suffix + "@example.com");
        User linked = persistUser("linked_" + suffix, "linked_" + suffix + "@example.com");
        User available = persistUser("dara_" + suffix, "dara_" + suffix + "@example.com");
        available.setPhone("099" + suffix);
        entityManager.persistAndFlush(available);

        // "linked" user already owns a tenant profile and must be excluded.
        Tenant tenant = Tenant.builder()
                .user(linked)
                .owner(owner)
                .tenantCode("TNC_" + suffix)
                .firstName("Linked")
                .lastName("User")
                .phone("088" + suffix)
                .status(TenantStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(tenant);

        Page<User> byName = userRepository.findAvailableForTenant("dara_" + suffix, PageRequest.of(0, 10));
        assertThat(byName.getContent()).hasSize(1);
        assertThat(byName.getContent().get(0).getId()).isEqualTo(available.getId());

        Page<User> byFullName = userRepository.findAvailableForTenant("dara", PageRequest.of(0, 10));
        assertThat(byFullName.getContent()).extracting(User::getId).contains(available.getId());

        Page<User> byEmail = userRepository.findAvailableForTenant("dara_" + suffix, PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).extracting(User::getId).contains(available.getId());

        Page<User> byPhone = userRepository.findAvailableForTenant("099" + suffix, PageRequest.of(0, 10));
        assertThat(byPhone.getContent()).extracting(User::getId).contains(available.getId());

        // The already-linked user never appears for ANY query.
        for (Page<User> result : java.util.List.of(byName, byFullName, byEmail, byPhone)) {
            assertThat(result.getContent()).extracting(User::getId).doesNotContain(linked.getId());
        }
    }

    @Test
    void findAllOwnersReturnsOnlyUsersHoldingOwnerRole() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Role ownerRole = findOrCreateOwnerRole(suffix);
        User owner = persistUser("owner_" + suffix, "owner_" + suffix + "@rental.com");
        User regular = persistUser("regular_" + suffix, "regular_" + suffix + "@rental.com");
        owner.setPhone("099_" + suffix);
        entityManager.persistAndFlush(owner);

        entityManager.persist(UserRole.builder()
                .id(new UserRoleId(owner.getId(), ownerRole.getId()))
                .user(owner)
                .role(ownerRole)
                .build());
        entityManager.flush();

        Page<User> bySearch = userRepository.findAllOwners("owner_" + suffix, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).extracting(User::getId)
                .contains(owner.getId())
                .doesNotContain(regular.getId());

        Page<User> byPhone = userRepository.findAllOwners("099_" + suffix, null, PageRequest.of(0, 10));
        assertThat(byPhone.getContent()).extracting(User::getId).contains(owner.getId());

        // The unfiltered owner list runs on the shared test database, which already
        // contains more owners than the default page size; use a full page so the
        // freshly created owner is guaranteed to be part of the result set.
        Page<User> allOwners = userRepository.findAllOwners(null, null, PageRequest.of(0, 1000));
        assertThat(allOwners.getContent()).extracting(User::getId).contains(owner.getId());
    }

    @Test
    void findAllOwnersFiltersByStatus() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Role ownerRole = findOrCreateOwnerRole(suffix);
        User activeOwner = persistUser("act_" + suffix, "act_" + suffix + "@rental.com");
        User inactiveOwner = persistUser("ina_" + suffix, "ina_" + suffix + "@rental.com");
        inactiveOwner.setStatus(UserStatus.INACTIVE);
        entityManager.persistAndFlush(inactiveOwner);

        entityManager.persist(UserRole.builder()
                .id(new UserRoleId(activeOwner.getId(), ownerRole.getId()))
                .user(activeOwner)
                .role(ownerRole)
                .build());
        entityManager.persist(UserRole.builder()
                .id(new UserRoleId(inactiveOwner.getId(), ownerRole.getId()))
                .user(inactiveOwner)
                .role(ownerRole)
                .build());
        entityManager.flush();

        Page<User> active = userRepository.findAllOwners(null, UserStatus.ACTIVE, PageRequest.of(0, 10));
        assertThat(active.getContent()).extracting(User::getId)
                .contains(activeOwner.getId())
                .doesNotContain(inactiveOwner.getId());
    }

    /**
     * Reuses the existing seeded OWNER role row when present (the dev database
     * seeds role rows outside the migrations), otherwise creates one so the test
     * never depends on unverifiable seed state.
     */
    private Role findOrCreateOwnerRole(String suffix) {
        List<Role> existing = entityManager.getEntityManager()
                .createQuery("SELECT r FROM Role r WHERE r.code = 'OWNER'", Role.class)
                .getResultList();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return entityManager.persistFlushFind(Role.builder()
                .name("Owner " + suffix)
                .code("OWNER")
                .isSystemRole(true)
                .build());
    }
}
