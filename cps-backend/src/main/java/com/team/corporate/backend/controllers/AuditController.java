package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.AuditEventResponse;
import com.team.corporate.backend.dto.PageResponse;
import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;
import com.team.corporate.backend.filter.AuditFilter;
import com.team.corporate.backend.filter.CorrelationIdFilter;
import com.team.corporate.backend.services.AuditEventsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {
    private final AuditEventsService events;

    public AuditController(AuditEventsService events) { this.events = events; }

    @GetMapping
    public PageResponse<AuditEventResponse> getEvents(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "q", required = false) @Size(max = 255) String q,
            @RequestParam(name = "actorId", required = false) UUID actorId,
            @RequestParam(name = "eventType", required = false) AuditEventType eventType,
            @RequestParam(name = "entityType", required = false) AuditEntityType entityType,
            @RequestParam(name = "entityId", required = false) UUID entityId,
            @RequestParam(name = "correlationId", required = false) @Pattern(regexp = CorrelationIdFilter.VALID_ID) String correlationId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "sort", defaultValue = "occurredAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction
    ) {
        return events.getEvents(new AuditFilter(q, actorId, eventType, entityType, entityId, correlationId, from, to),
                page, size, sort, direction);
    }
}
