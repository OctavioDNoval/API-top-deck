package org.example.topdeckapi;

import org.example.topdeckapi.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

// The whole context starts and Flyway migrates an empty PostgreSQL (Hibernate validates the schema).
class TopDeckApiApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}
