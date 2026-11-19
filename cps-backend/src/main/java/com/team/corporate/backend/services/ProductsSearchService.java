package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.ProductResponse;
import com.team.corporate.backend.repositories.SpringProductsRepository;
import com.team.corporate.backend.search.ProductFilter;
import com.team.corporate.backend.search.SearchPaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ProductsSearchService {
    private static final Set<String> SORT_FIELDS = Set.of("sku", "name", "price");
    private final SpringProductsRepository products;

    public ProductsSearchService(SpringProductsRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductFilter filter, int page, int size, Sort sort) {
        var pageable = SearchPaging.of(page, size, sort, SORT_FIELDS, "sku");

        return products.findAll(filter.toSpecification(), pageable).map(ProductResponse::of);
    }

}
