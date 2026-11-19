package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserFullNameMigrationTest {
    @Test
    void migrationBackfillsExistingNamesAndRequiresNamesForNewUsers() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:user-full-name-migration")) {
            UUID id = UUID.randomUUID();
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (id UUID PRIMARY KEY, username VARCHAR(255) NOT NULL)");
            }
            try (var statement = connection.prepareStatement("INSERT INTO users VALUES (?, ?)")) {
                statement.setObject(1, id);
                statement.setString(2, "existing-user");
                statement.executeUpdate();
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("database/migrations/V14__add_user_full_name.sql"));

            try (var statement = connection.prepareStatement("SELECT username, full_name FROM users WHERE id = ?")) {
                statement.setObject(1, id);
                try (var rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals("existing-user", rows.getString("username"));
                    assertEquals("existing-user", rows.getString("full_name"));
                }
            }
            try (var statement = connection.createStatement()) {
                assertThrows(SQLException.class,
                        () -> statement.execute("INSERT INTO users (id, username) VALUES (RANDOM_UUID(), 'missing-name')"));
                assertEquals(1, statement.executeUpdate(
                        "INSERT INTO users (id, username, full_name) VALUES (RANDOM_UUID(), 'new-user', 'New User')"));
            }
        }
    }
}
