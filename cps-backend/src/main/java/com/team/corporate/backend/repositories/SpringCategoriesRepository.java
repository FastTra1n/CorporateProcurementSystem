package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Category;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.repositories.CategoriesRepository;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringCategoriesRepository extends CategoriesRepository, ListCrudRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    @Override
    @Transactional
    @NotNull
    default Category add(@NotNull Category category) {
        return save(category);
    }

    @Override
    @NotNull
    default List<Category> getAll() {
        return findAll();
    }

    @Override
    @NonNull
    Page<Category> findAll(@NonNull Specification<Category> spec, @NonNull Pageable page);

    @Override
    @NotNull
    default Optional<Category> getById(@NotNull UUID id) {
        return findById(id);
    }

    @Override
    @Transactional
    @NotNull
    default Category update(@NotNull Category category) {
        return save(category);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
