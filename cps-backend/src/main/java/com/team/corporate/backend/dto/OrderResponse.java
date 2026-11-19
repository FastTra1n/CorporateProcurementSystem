package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal totalSum
) {
    public static OrderResponse of(Order order, BigDecimal totalSum) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                totalSum
        );
    }
}
