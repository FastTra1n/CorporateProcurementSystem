package com.team.corporate.backend.dto;

import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEvent;
import com.team.corporate.backend.entities.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, Instant occurredAt, UUID actorId, String actorUsername,
                                 AuditEventType eventType, AuditEntityType entityType, UUID entityId,
                                 String description, String correlationId) {
    public static AuditEventResponse of(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getActorId(), event.getActorUsername(),
                event.getEventType(), event.getEntityType(), event.getEntityId(), event.getDescription(), event.getCorrelationId());
    }
}
