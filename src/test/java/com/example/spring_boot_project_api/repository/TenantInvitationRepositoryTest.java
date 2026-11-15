package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.TenantInvitation;
import com.example.spring_boot_project_api.model.TenantInvitationStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.model.UserHouseAccessId;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.util.InvitationTokenUtil;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class TenantInvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantInvitationRepository tenantInvitationRepository;

    private User persistUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(user);
    }

    private House persistHouse(User owner, String name) {
        House house = House.builder()
                .owner(owner)
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .code("H-" + name.length())
                .address("Phnom Penh")
                .status(HouseStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(house);
    }

    private TenantInvitation persistInvitation(User manager, House house, String email,
            TenantInvitationStatus status, LocalDateTime expiresAt) {
        TenantInvitation invitation = TenantInvitation.builder()
                .manager(manager)
                .house(house)
                .email(email)
                .tokenHash(InvitationTokenUtil.hashToken("raw-" + UUID.randomUUID()))
                .expiresAt(expiresAt)
                .status(status)
                .build();
        return entityManager.persistFlushFind(invitation);
    }

    @Test
    void findByTokenHashFindsInvitation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager = persistUser("manager_" + suffix);
        House house = persistHouse(manager, "Hash House");
        TenantInvitation invitation = persistInvitation(manager, house,
                "hash_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));

        assertThat(tenantInvitationRepository.findByTokenHash(invitation.getTokenHash()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(invitation.getId());
                    assertThat(found.getHouse().getName()).isEqualTo("Hash House");
                    assertThat(found.getStatus()).isEqualTo(TenantInvitationStatus.PENDING);
                });
    }

    @Test
    void markUsedClaimIsAtomicAndWinsOnlyOnce() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager = persistUser("manager_" + suffix);
        House house = persistHouse(manager, "Claim House");
        TenantInvitation invitation = persistInvitation(manager, house,
                "claim_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));
        entityManager.getEntityManager().clear();

        LocalDateTime now = LocalDateTime.now();
        int first = tenantInvitationRepository.markUsedIfPendingAndNotExpired(
                invitation.getId(), now, now);
        int second = tenantInvitationRepository.markUsedIfPendingAndNotExpired(
                invitation.getId(), now, now);

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);

        TenantInvitation reloaded = entityManager.find(TenantInvitation.class, invitation.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TenantInvitationStatus.USED);
        assertThat(reloaded.getUsedAt()).isNotNull();
    }

    @Test
    void markUsedRejectsExpiredInvitation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager = persistUser("manager_" + suffix);
        House house = persistHouse(manager, "Expiry House");
        TenantInvitation invitation = persistInvitation(manager, house,
                "expired_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().minusMinutes(1));
        entityManager.getEntityManager().clear();

        LocalDateTime now = LocalDateTime.now();
        int claimed = tenantInvitationRepository.markUsedIfPendingAndNotExpired(
                invitation.getId(), now, now);

        assertThat(claimed).isZero();
        TenantInvitation reloaded = entityManager.find(TenantInvitation.class, invitation.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TenantInvitationStatus.PENDING);
    }

    @Test
    void findAllByManagerIdOrdersByCreatedAtDescAndFiltersCreator() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager = persistUser("manager_" + suffix);
        User other = persistUser("other_" + suffix);
        House house = persistHouse(manager, "List House");

        TenantInvitation older = persistInvitation(manager, house,
                "older_" + suffix + "@example.com", TenantInvitationStatus.USED,
                LocalDateTime.now().plusMinutes(5));
        TenantInvitation newer = persistInvitation(other, house,
                "newer_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));

        assertThat(tenantInvitationRepository.findAllByManagerIdOrderByCreatedAtDesc(manager.getId()))
                .extracting(TenantInvitation::getId)
                .contains(older.getId())
                .doesNotContain(newer.getId());
    }

    @Test
    void findAllVisibleScopesByHouseOwnerOrHouseAccessIsolatingManagers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner = persistUser("owner_" + suffix);
        User managerA = persistUser("managerA_" + suffix);
        User managerB = persistUser("managerB_" + suffix);
        House houseA = persistHouse(owner, "AAA House");
        House houseB = persistHouse(owner, "BB House");

        TenantInvitation inviteA = persistInvitation(managerA, houseA,
                "invite_a_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));
        TenantInvitation inviteB = persistInvitation(managerB, houseB,
                "invite_b_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));

        // managerA has access only to houseA; managerB only to houseB.
        persistHouseAccess(managerA, houseA);
        persistHouseAccess(managerB, houseB);
        entityManager.getEntityManager().clear();

        assertThat(tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(owner.getId()))
                .extracting(TenantInvitation::getId)
                .contains(inviteA.getId(), inviteB.getId());

        assertThat(tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(managerA.getId()))
                .extracting(TenantInvitation::getId)
                .contains(inviteA.getId())
                .doesNotContain(inviteB.getId());

        assertThat(tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(managerB.getId()))
                .extracting(TenantInvitation::getId)
                .contains(inviteB.getId())
                .doesNotContain(inviteA.getId());
    }

    @Test
    void ownerIssuedInvitationIsVisibleToTheManagingManagerButNotOthers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner = persistUser("owner_" + suffix);
        User managerA = persistUser("managerA_" + suffix);
        User managerB = persistUser("managerB_" + suffix);
        House houseA = persistHouse(owner, "CCC House");
        House houseB = persistHouse(owner, "DD House");

        // The owner issues the invitation on house A; no manager created it.
        TenantInvitation ownerIssued = persistInvitation(owner, houseA,
                "owner_issued_" + suffix + "@example.com", TenantInvitationStatus.PENDING,
                LocalDateTime.now().plusMinutes(5));

        persistHouseAccess(managerA, houseA);
        persistHouseAccess(managerB, houseB);
        entityManager.getEntityManager().clear();

        assertThat(tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(managerA.getId()))
                .extracting(TenantInvitation::getId)
                .contains(ownerIssued.getId());

        assertThat(tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(managerB.getId()))
                .extracting(TenantInvitation::getId)
                .doesNotContain(ownerIssued.getId());
    }

    private UserHouseAccess persistHouseAccess(User user, House house) {
        UserHouseAccess access = UserHouseAccess.builder()
                .id(new UserHouseAccessId(user.getId(), house.getId()))
                .user(user)
                .house(house)
                .build();
        return entityManager.persistFlushFind(access);
    }
}