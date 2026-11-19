package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.Size;

public record UpdateOrderRequest(
        OrderStatus status,
        @Size(max = EntityValidation.TEXT_MAX_LENGTH) String notes
) {}
