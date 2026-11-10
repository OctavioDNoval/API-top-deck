package org.example.topdeckapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Needs a running PostgreSQL and the app env vars; skipped otherwise.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class TopDeckApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
