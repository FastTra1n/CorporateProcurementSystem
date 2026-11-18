package com.team.corporate.backend.search;

import com.team.corporate.core.entities.Product;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

public record ProductFilter (
        String name,
        UUID categoryId,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice

        ) {
    public Specification<Product> toSpecification() {
        if(minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0 ) {
            throw new IllegalArgumentException("Минимальная цена не может быть больше максимальной цены.");
        }

        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();


            if (name != null && !name.isBlank()) {
                var text = name.strip().toLowerCase(Locale.ROOT);

                conditions.add(cb.gt(
                        cb.locate(cb.lower(root.get("name")), text),
                        0));
            }
            if (categoryId != null) {
                conditions.add(cb.equal(
                        root.get("category").get("id"),
                        categoryId));
            }

            if(minPrice != null) {
                conditions.add(cb.ge(
                        root.get("price"),
                        minPrice));
            }

            if(maxPrice != null) {
                conditions.add(cb.le(
                        root.get("price"),
                        maxPrice));
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
