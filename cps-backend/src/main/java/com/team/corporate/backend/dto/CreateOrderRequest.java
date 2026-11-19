package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID userId,
        @NotNull OrderStatus status,
        @Size(max = EntityValidation.TEXT_MAX_LENGTH) String notes
) {}
