package com.team.corporate.backend.filter;

import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditFilter(String q, UUID actorId, AuditEventType eventType, AuditEntityType entityType,
                          UUID entityId, String correlationId, Instant from, Instant to) {
}
