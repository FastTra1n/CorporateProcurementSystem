package com.team.corporate.backend.search;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public record AuditLogFilter(
        UUID orderId,
        OrderStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore
) {
    public Specification<AuditLog> toSpecification() {
        if (createdFrom != null && createdBefore != null && !createdFrom.isBefore(createdBefore)) {
            throw new IllegalArgumentException("Начальная дата должна быть раньше конечной.");
        }

        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();

            if (orderId != null) {
                conditions.add(cb.equal(root.get("order").get("id"), orderId));
            }
            if (status != null) {
                conditions.add(cb.equal(root.get("order").get("status"), status));
            }
            if (createdFrom != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<Instant>get("changedAt"), createdFrom));
            }
            if (createdBefore != null) {
                conditions.add(cb.lessThan(root.<Instant>get("changedAt"), createdBefore));
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
