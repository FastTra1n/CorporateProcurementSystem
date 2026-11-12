package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Inventory;
import com.team.corporate.core.repositories.InventoriesRepository;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
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
public interface SpringInventoriesRepository extends InventoriesRepository, ListCrudRepository<Inventory, UUID> {
    String SELECT_QUERY = "SELECT i FROM Inventory i JOIN FETCH i.product p LEFT JOIN FETCH p.category JOIN FETCH i.warehouse";

    @Override
    @Transactional
    @NotNull
    default Inventory add(@NotNull Inventory inventory) {
        return save(inventory);
    }

    @Override
    @Query(SELECT_QUERY)
    @NotNull
    List<Inventory> getAll();

    @Override
    @Query(SELECT_QUERY + " WHERE i.id = :id")
    @NotNull
    Optional<Inventory> getById(@NotNull @Param("id") UUID id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    @NotNull
    Optional<Inventory> getByIdForUpdate(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    @NotNull
    Optional<Inventory> getByProductAndWarehouseIds(
            @NotNull @Param("productId") UUID productId,
            @NotNull @Param("warehouseId") UUID warehouseId
    );

    @Override
    @Query(SELECT_QUERY + " WHERE i.product.id = :id")
    @NotNull
    List<Inventory> getByProductId(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE i.warehouse.id = :id")
    @NotNull
    List<Inventory> getByWarehouseId(@NotNull @Param("id") UUID id);

    @Override
    @Transactional
    @NotNull
    default Inventory update(@NotNull Inventory inventory) {
        return save(inventory);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}

