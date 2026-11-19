package com.team.corporate.backend.search;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.entities.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public record OrderFilter(
        UUID userId,
        OrderStatus status,
        Instant createdFrom,
        Instant createdBefore,
        @PositiveOrZero BigDecimal minTotal,
        @PositiveOrZero BigDecimal maxTotal
) {
    public Specification<Order> toSpecification(){
        if(createdFrom != null && createdBefore != null && !createdFrom.isBefore(createdBefore) ){
            throw new IllegalArgumentException("Начальная дата поиска должен быть раньше конечной даты.");
        }

        if (minTotal != null && maxTotal != null && minTotal.compareTo(maxTotal) > 0){
            throw new IllegalArgumentException("Начальное значение суммы заказа должно быть больше конечной суммы.");
        }

        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();

            if(userId != null) {
                conditions.add(cb.equal(root.get("user").get("id"), userId));
            }

            if(status != null ){
                conditions.add(cb.equal(root.get("status"), status));
            }

            if(createdFrom != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));

            }

            if(createdBefore != null) {
                conditions.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }

            if(minTotal != null || maxTotal != null){
                var totalQuery = query.subquery(BigDecimal.class);
                var item = totalQuery.from(OrderItem.class);

                var lineTotal = cb.prod(
                        item.get("purchasePrice"),
                        item.<Integer>get("quantity").as(BigDecimal.class));

                totalQuery.select(
                        cb.coalesce(cb.sum(lineTotal), BigDecimal.ZERO)
                );

                totalQuery.where(cb.equal(item.get("order"), root));

                if(minTotal != null){
                    conditions.add(cb.ge(totalQuery, minTotal));
                }

                if(maxTotal != null){
                    conditions.add(cb.le(totalQuery, maxTotal));
                }
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
