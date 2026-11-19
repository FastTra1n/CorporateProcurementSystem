package com.team.corporate.backend.search;

import com.team.corporate.core.entities.Warehouse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

public record WarehouseFilter(String name, String location) {
    public Specification<Warehouse> toSpecification() {
        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();

            if (name != null && !name.isBlank()) {
                var text = name.strip().toLowerCase(Locale.ROOT);
                conditions.add(cb.gt(cb.locate(cb.lower(root.get("name")), text), 0));
            }
            if (location != null && !location.isBlank()) {
                var text = location.strip().toLowerCase(Locale.ROOT);
                conditions.add(cb.gt(cb.locate(cb.lower(root.get("location")), text), 0));
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
