package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.AuditLogResponse;
import com.team.corporate.backend.repositories.SpringAuditLogsRepository;
import com.team.corporate.backend.search.AuditLogFilter;
import com.team.corporate.backend.search.SearchPaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogsSearchService {
    private static final Set<String> SORT_FIELDS = Set.of("changedAt");

    private final SpringAuditLogsRepository audit;

    public AuditLogsSearchService(SpringAuditLogsRepository audit) {
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(AuditLogFilter filter, int page, int size, Sort sort) {
        var actualSort = sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "changedAt");
        var pageable = SearchPaging.of(page, size, actualSort, SORT_FIELDS, "id");
        return audit.findAll(filter.toSpecification(), pageable).map(AuditLogResponse::of);
    }
}
