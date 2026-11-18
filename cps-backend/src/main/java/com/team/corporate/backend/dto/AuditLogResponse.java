package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID orderId,
        UUID changedByUserId,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        Instant changedAt
) {
    public static AuditLogResponse of(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOrder().getId(),
                log.getChangedBy().getId(),
                log.getOldStatus(),
                log.getNewStatus(),
                log.getChangedAt()
        );
    }
}
