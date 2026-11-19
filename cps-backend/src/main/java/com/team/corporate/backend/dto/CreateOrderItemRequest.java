package com.team.corporate.backend.dto;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive @Max(EntityValidation.ORDER_LINE_MAX_QUANTITY) Integer quantity,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal purchasePrice
) {}
