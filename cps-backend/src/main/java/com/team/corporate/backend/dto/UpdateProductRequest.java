package com.team.corporate.backend.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest (
        @Size(max = 255) String name,
        @Size(max = 255) String sku,
        UUID categoryId,
        @Positive BigDecimal price
        ) { }
