package com.team.corporate.backend.audit;

import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;
import com.team.corporate.backend.filter.CorrelationIdFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Component
public class AuditEventWriter {
    private static final String INSERT = """
            INSERT INTO audit_events (id, occurred_at, actor_id, actor_username, event_type,
                                      entity_type, entity_id, description, correlation_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AuditEventWriter(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEventType eventType, AuditEntityType entityType, UUID entityId, String description) {
        insert(eventType, entityType, entityId, description, currentActor());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAs(AuditEventType eventType, AuditEntityType entityType, UUID entityId,
                         String description, UUID actorId, String actorUsername) {
        insert(eventType, entityType, entityId, description, new Actor(actorId, actorUsername));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFailed(String attemptedUsername) {
        insert(AuditEventType.LOGIN_FAILURE, AuditEntityType.USER, null,
                "Неуспешная попытка входа.", new Actor(null, attemptedUsername));
    }

    // JDBC uses the transaction's connection, including inside JPA flush callbacks.
    // It does not enqueue new JPA actions or recursively audit audit records.
    private void insert(AuditEventType eventType, AuditEntityType entityType, UUID entityId,
                        String description, Actor actor) {
        jdbc.update(INSERT, UUID.randomUUID(), Timestamp.from(clock.instant()), actor.id(), actor.username(),
                eventType.name(), entityType.name(), entityId, description, CorrelationIdFilter.currentId());
    }

    private static Actor currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return new Actor(null, "system");
        UUID id = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            id = UUID.fromString(jwt.getToken().getSubject());
        }
        return new Actor(id, authentication.getName());
    }

    private record Actor(UUID id, String username) { }
}
