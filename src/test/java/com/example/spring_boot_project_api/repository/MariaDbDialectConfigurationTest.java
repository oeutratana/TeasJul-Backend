package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.dialect.MariaDBDialect;
import org.junit.jupiter.api.Test;

class MariaDbDialectConfigurationTest {

    @Test
    void configuredDialectGeneratesAliasFreePessimisticWriteLock() throws IOException {
        Properties properties = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        }

        assertThat(properties.getProperty("spring.jpa.database-platform"))
                .isEqualTo(MariaDBDialect.class.getName());

        MariaDBDialect dialect = new MariaDBDialect();
        assertThat(dialect.getForUpdateString("r1_0")).isEqualTo(" for update");
    }
}
