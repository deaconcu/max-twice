package com.prosper.learn.web.v1.controller;

import com.prosper.learn.shared.infrastructure.config.SystemDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(
    classes = com.prosper.learn.web.application.Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public abstract class BaseControllerTest {

    @Autowired
    private SystemDomainService systemDomainService;

    private static boolean configReloaded = false;

    @BeforeEach
    void reloadSystemConfig() {
        if (!configReloaded) {
            synchronized (BaseControllerTest.class) {
                if (!configReloaded) {
                    systemDomainService.reload();
                    configReloaded = true;
                }
            }
        }
    }
}
