package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Inventory;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        UUID warehouseId,
        int quantity
) {
    public static InventoryResponse of(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getWarehouse().getId(),
                inventory.getQuantity()
        );
    }
}
