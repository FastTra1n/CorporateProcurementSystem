package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.WarehouseResponse;
import com.team.corporate.backend.repositories.SpringWarehousesRepository;
import com.team.corporate.backend.search.SearchPaging;
import com.team.corporate.backend.search.WarehouseFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class WarehousesSearchService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "location");

    private final SpringWarehousesRepository warehouses;

    public WarehousesSearchService(SpringWarehousesRepository warehouses) {
        this.warehouses = warehouses;
    }

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> search(WarehouseFilter filter, int page, int size, Sort sort) {
        var pageable = SearchPaging.of(page, size, sort, SORT_FIELDS, "id");
        return warehouses.findAll(filter.toSpecification(), pageable).map(WarehouseResponse::of);
    }
}
