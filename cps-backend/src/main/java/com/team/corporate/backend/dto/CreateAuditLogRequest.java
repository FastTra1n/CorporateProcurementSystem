package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAuditLogRequest(
        @NotNull UUID orderId,
        @NotNull OrderStatus oldStatus,
        @NotNull OrderStatus newStatus
) {}
