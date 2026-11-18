package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String sku,
        @NotNull UUID categoryId,
        @NotNull @Positive BigDecimal price
        ) {}
