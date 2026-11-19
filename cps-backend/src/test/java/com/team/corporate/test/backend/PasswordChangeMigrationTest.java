package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PasswordChangeMigrationTest {
    @Test
    void migrationPreservesExistingAccountsAndRequiresChangesForNewCredentials() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:password-change-migration")) {
            UUID existing = UUID.randomUUID();
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE user_credentials (user_id UUID PRIMARY KEY, password_hash VARCHAR(255) NOT NULL)");
            }
            try (var statement = connection.prepareStatement("INSERT INTO user_credentials VALUES (?, ?)")) {
                statement.setObject(1, existing);
                statement.setString(2, "existing-hash");
                statement.executeUpdate();
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("database/migrations/V13__require_password_change.sql"));

            try (var statement = connection.prepareStatement("SELECT password_hash, must_change_password FROM user_credentials WHERE user_id = ?")) {
                statement.setObject(1, existing);
                try (var rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals("existing-hash", rows.getString("password_hash"));
                    assertFalse(rows.getBoolean("must_change_password"));
                    assertFalse(rows.wasNull());
                }
            }

            UUID created = UUID.randomUUID();
            try (var statement = connection.prepareStatement("INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)")) {
                statement.setObject(1, created);
                statement.setString(2, "new-hash");
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("SELECT must_change_password FROM user_credentials WHERE user_id = ?")) {
                statement.setObject(1, created);
                try (var rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    assertTrue(rows.getBoolean(1));
                }
            }
        }
    }
}
