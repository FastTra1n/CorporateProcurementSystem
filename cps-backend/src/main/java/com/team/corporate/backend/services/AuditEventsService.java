package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.AuditEventResponse;
import com.team.corporate.backend.dto.PageResponse;
import com.team.corporate.backend.entities.AuditEvent;
import com.team.corporate.backend.filter.AuditFilter;
import com.team.corporate.backend.filter.CorrelationIdFilter;
import com.team.corporate.backend.repositories.AuditEventsRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

@Service
public class AuditEventsService {
    private static final Set<String> SORT_FIELDS = Set.of("occurredAt", "id", "actorId", "actorUsername",
            "eventType", "entityType", "entityId", "correlationId");
    private final AuditEventsRepository events;

    public AuditEventsService(AuditEventsRepository events) {
        this.events = events;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> getEvents(AuditFilter filter, int page, int size, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Недопустимый номер или размер страницы.");
        }
        if (!SORT_FIELDS.contains(sort)) throw new IllegalArgumentException("Неизвестное поле сортировки.");
        if (filter.q() != null && filter.q().length() > 255) throw new IllegalArgumentException("Слишком длинная строка поиска.");
        if (filter.correlationId() != null && !filter.correlationId().matches(CorrelationIdFilter.VALID_ID)) {
            throw new IllegalArgumentException("Недопустимый Correlation ID.");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("Начало периода должно предшествовать его концу.");
        }
        Sort order = Sort.by(Sort.Direction.fromString(direction), sort);
        if (!sort.equals("id")) order = order.and(Sort.by("id"));
        return PageResponse.of(events.findAll(specification(filter), PageRequest.of(page, size, order))
                .map(AuditEventResponse::of));
    }

    private static Specification<AuditEvent> specification(AuditFilter filter) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.actorId() != null) predicates.add(builder.equal(root.get("actorId"), filter.actorId()));
            if (filter.eventType() != null) predicates.add(builder.equal(root.get("eventType"), filter.eventType()));
            if (filter.entityType() != null) predicates.add(builder.equal(root.get("entityType"), filter.entityType()));
            if (filter.entityId() != null) predicates.add(builder.equal(root.get("entityId"), filter.entityId()));
            if (filter.correlationId() != null) predicates.add(builder.equal(root.get("correlationId"), filter.correlationId()));
            if (filter.from() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            if (filter.to() != null) predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            if (filter.q() != null && !filter.q().isBlank()) {
                String value = filter.q().strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + value + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("actorUsername")), pattern, '\\'),
                        builder.like(builder.lower(root.get("description")), pattern, '\\'),
                        builder.like(builder.lower(root.get("correlationId")), pattern, '\\')));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
