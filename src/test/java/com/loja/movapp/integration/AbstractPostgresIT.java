package com.loja.movapp.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

abstract class AbstractPostgresIT {

    private static final boolean USE_EXTERNAL =
            "true".equalsIgnoreCase(System.getenv("IT_USE_EXTERNAL_POSTGRES"));

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (USE_EXTERNAL) {
            POSTGRES = null;
        } else {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        if (USE_EXTERNAL) {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/movapp_test");
            registry.add("spring.datasource.username", () -> "test");
            registry.add("spring.datasource.password", () -> "test");
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}
