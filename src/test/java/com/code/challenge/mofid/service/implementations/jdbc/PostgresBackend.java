package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Application;
import com.code.challenge.mofid.Profiles;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class PostgresBackend {

    private static ConfigurableApplicationContext context;

    private PostgresBackend() {
    }

    static synchronized ConfigurableApplicationContext context() {
        if (context == null) {
            PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
            postgres.start();
            context = new SpringApplicationBuilder(Application.class)
                    .profiles(Profiles.POSTGRES)
                    .run("--spring.datasource.url=" + postgres.getJdbcUrl(),
                            "--spring.datasource.username=" + postgres.getUsername(),
                            "--spring.datasource.password=" + postgres.getPassword(),
                            "--spring.main.web-application-type=none");
        }
        return context;
    }

    static <T> T bean(Class<T> type) {
        return context().getBean(type);
    }

    static void emptyTables() {
        bean(JdbcClient.class).sql("TRUNCATE accounts, transactions").update();
    }
}
