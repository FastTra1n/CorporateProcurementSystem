package com.team.corporate.backend.search;

import com.team.corporate.core.entities.Inventory;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public record InventoryFilter(
        UUID productId,
        UUID warehouseId,
        String productName,
        String warehouseName,
        @PositiveOrZero Integer minQuantity,
        @PositiveOrZero Integer maxQuantity
) {
    public Specification<Inventory> toSpecification() {
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
            throw new IllegalArgumentException("Минимальный остаток не может быть больше максимального.");
        }

        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();

            if (productId != null) {
                conditions.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (warehouseId != null) {
                conditions.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (productName != null && !productName.isBlank()) {
                var text = productName.strip().toLowerCase(Locale.ROOT);
                conditions.add(cb.gt(cb.locate(cb.lower(root.get("product").get("name")), text), 0));
            }
            if (warehouseName != null && !warehouseName.isBlank()) {
                var text = warehouseName.strip().toLowerCase(Locale.ROOT);
                conditions.add(cb.gt(cb.locate(cb.lower(root.get("warehouse").get("name")), text), 0));
            }
            if (minQuantity != null) {
                conditions.add(cb.ge(root.get("quantity"), minQuantity));
            }
            if (maxQuantity != null) {
                conditions.add(cb.le(root.get("quantity"), maxQuantity));
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
