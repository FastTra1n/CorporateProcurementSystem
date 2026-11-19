package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull UUID productId,
        @NotNull UUID warehouseId,
        @NotNull @PositiveOrZero Integer quantity
) {}
