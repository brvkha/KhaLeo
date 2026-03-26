package com.khaleo.flashcard.integration.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;

public abstract class IntegrationPersistenceTestBase {

    // Shared DB container for persistence tests; FSRS v6 scheduling assertions must remain algorithm-invariant
    // across rich-card content changes and should not depend on content projection fields.

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("khaleo_flashcard_test")
            .withUsername("khaleo")
            .withPassword("khaleo");

    static {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
