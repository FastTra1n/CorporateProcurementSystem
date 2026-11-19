package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.InventoryResponse;
import com.team.corporate.backend.repositories.SpringInventoriesRepository;
import com.team.corporate.backend.search.InventoryFilter;
import com.team.corporate.backend.search.SearchPaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class InventoriesSearchService {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "quantity", "product.name", "product.sku", "warehouse.name");

    private final SpringInventoriesRepository inventories;

    public InventoriesSearchService(SpringInventoriesRepository inventories) {
        this.inventories = inventories;
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> search(InventoryFilter filter, int page, int size, Sort sort) {
        var pageable = SearchPaging.of(page, size, sort, SORT_FIELDS, "id");
        return inventories.findAll(filter.toSpecification(), pageable).map(InventoryResponse::of);
    }
}
