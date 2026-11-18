package com.team.corporate.backend.dto;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateOrderItemRequest(
        @Positive @Max(EntityValidation.ORDER_LINE_MAX_QUANTITY) Integer quantity,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal purchasePrice
) {}
