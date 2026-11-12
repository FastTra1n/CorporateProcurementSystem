package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Product;
import com.team.corporate.core.repositories.ProductsRepository;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringProductsRepository extends ProductsRepository, ListCrudRepository<Product, UUID> {
    String SELECT_QUERY = "SELECT p FROM Product p LEFT JOIN FETCH p.category";

    @Override
    @Transactional
    @NotNull
    default Product add(@NonNull Product product) {
        return save(product);
    }

    @Override
    @Query(SELECT_QUERY)
    @NotNull
    List<Product> getAll();

    @Override
    @Query(SELECT_QUERY + " WHERE p.id = :id")
    @NotNull
    Optional<Product> getById(@NotNull @Param("id") UUID id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    @NotNull
    Optional<Product> getByIdForUpdate(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE p.sku = :sku")
    @NotNull
    Optional<Product> getBySKU(@NotNull @Param("sku") String sku);

    @Override
    @Transactional
    @NotNull
    default Product update(@NotNull Product product) {
        return save(product);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
