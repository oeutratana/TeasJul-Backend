package com.example.spring_boot_project_api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the complete Flyway migration chain against a brand-new, disposable MySQL
 * container and lets Hibernate validate the resulting schema (ddl-auto=validate).
 *
 * <p>This is the only test that exercises the migrations themselves against an empty
 * database instead of an externally provisioned one, so it never touches a developer
 * or CI database. It is skipped automatically when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        "spring.jpa.show-sql=false"
})
class FreshDatabaseMigrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("teasjul_fresh_" + UUID.randomUUID().toString().replace("-", ""))
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideConfiguration(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.clean-disabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void freshDatabase_createsTheFourPreviouslyOrphanedTables() {
        assertThat(tableExists("room_types")).as("room_types").isTrue();
        assertThat(tableExists("rooms")).as("rooms").isTrue();
        assertThat(tableExists("expense_categories")).as("expense_categories").isTrue();
        assertThat(tableExists("expenses")).as("expenses").isTrue();
    }

    @Test
    void freshDatabase_expensesHasNoObsoletePaymentMethodRelationship() {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'expenses' "
                        + "AND column_name = 'payment_method_id'",
                Integer.class);
        assertThat(columnCount).isZero();

        Integer fkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage "
                        + "WHERE table_schema = DATABASE() AND table_name = 'expenses' "
                        + "AND referenced_table_name = 'payment_methods'",
                Integer.class);
        assertThat(fkCount).isZero();
    }

    @Test
    void freshDatabase_flywayHistoryMarksRoomAndExpenseMigrationsSuccessful() {
        for (String version : new String[] {"8", "9", "68", "69"}) {
            Integer successful = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1",
                    Integer.class, version);
            assertThat(successful).as("successful migration V%s", version).isEqualTo(1);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count == 1;
    }
}
