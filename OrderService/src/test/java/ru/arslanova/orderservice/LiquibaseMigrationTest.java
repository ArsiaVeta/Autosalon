package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        assertThat(tableExists("clients")).isTrue();
        assertThat(tableExists("orders")).isTrue();
        assertThat(tableExists("order_components")).isTrue();
        assertThat(tableExists("outbox_events")).isTrue();
        assertThat(tableExists("processed_messages")).isTrue();
        assertThat(tableExists("test_drive_applications")).isTrue();
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
