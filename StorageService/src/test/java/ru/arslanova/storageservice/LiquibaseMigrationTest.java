package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.enabled=true",
        "spring.liquibase.drop-first=true",
        "spring.liquibase.change-log=classpath:changelog/db.changelog-master.yaml",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
public class LiquibaseMigrationTest extends AbstractPostgresIntegrationTest {
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            byte[] key = "test-secret-test-secret-test-secret-key".getBytes();
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allTablesShouldBeCreatedByLiquibase() {
        assertThat(tableExists("car_models")).isTrue();
        assertThat(tableExists("parts")).isTrue();
        assertThat(tableExists("car_model_components")).isTrue();
        assertThat(tableExists("assembly_orders")).isTrue();
        assertThat(tableExists("processed_messages")).isTrue();
    }

    @Test
    void seedDataShouldBeLoaded() {
        Integer parts = jdbc.queryForObject("SELECT COUNT(*) FROM parts", Integer.class);
        Integer cars = jdbc.queryForObject("SELECT COUNT(*) FROM car_models", Integer.class);
        Integer comps = jdbc.queryForObject("SELECT COUNT(*) FROM car_model_components", Integer.class);

        assertThat(parts).isGreaterThanOrEqualTo(5);
        assertThat(cars).isGreaterThanOrEqualTo(2);
        assertThat(comps).isGreaterThanOrEqualTo(4);
    }

    @Test
    void changelogShouldBeRecorded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(5);
    }

    private boolean tableExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = LOWER(?)",
                Integer.class, name);
        return count != null && count > 0;
    }
}
