package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.CategoryResponse;
import com.team.corporate.backend.repositories.SpringCategoriesRepository;
import com.team.corporate.backend.search.CategoryFilter;
import com.team.corporate.backend.search.SearchPaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CategoriesSearchService {
    private static final Set<String> SORT_FIELDS = Set.of("name");

    private final SpringCategoriesRepository categories;

    public CategoriesSearchService(SpringCategoriesRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(
            CategoryFilter filter,
            int page,
            int size,
            Sort sort
    ) {
        var pageable = SearchPaging.of(page, size, sort, SORT_FIELDS, "id");
        return categories.findAll(filter.toSpecification(), pageable).map(CategoryResponse::of);
    }

}
