package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        BigDecimal purchasePrice
) {
    public static OrderItemResponse of(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getProduct().getId(),
                item.getQuantity(),
                item.getPurchasePrice()
        );
    }
}
