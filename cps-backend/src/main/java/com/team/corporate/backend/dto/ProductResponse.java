package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        UUID categoryId
) {
    public static ProductResponse of(Product product) {
        var category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                category == null ? null : category.getId()
        );
    }
}
