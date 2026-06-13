package com.cramer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context smoke test (cutover milestone). Boots the entire Spring context — every module's
 * controllers, services, ports, security and web config — with {@code ddl-auto=validate} against
 * the live Supabase schema, proving the beans wire together with no conflicts and that all entity
 * mappings match the frozen schema (SPEC-00 §5).
 *
 * <p>Runs only when {@code SPRING_DATASOURCE_URL} is present in the environment (i.e. launched
 * with the root {@code .env} loaded). The default offline {@code mvn test} skips it.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
class ContextLoadsTest {

    @Test
    void contextLoads() {
        // Success == the application context started cleanly.
    }
}
