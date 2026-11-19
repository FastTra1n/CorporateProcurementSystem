package com.team.corporate.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_username", length = 255, updatable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_type", length = 40, nullable = false, updatable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", length = 40, nullable = false, updatable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "description", length = 1000, nullable = false, updatable = false)
    private String description;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    protected AuditEvent() {
    }

    public UUID getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getActorId() { return actorId; }
    public String getActorUsername() { return actorUsername; }
    public AuditEventType getEventType() { return eventType; }
    public AuditEntityType getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public String getCorrelationId() { return correlationId; }
}
