package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Warehouse;
import com.team.corporate.core.repositories.WarehousesRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringWarehousesRepository extends WarehousesRepository, ListCrudRepository<Warehouse, UUID> {
    @Override
    @Transactional
    @NotNull
    default Warehouse add(@NotNull Warehouse warehouse) {
        return save(warehouse);
    }

    @Override
    @NotNull
    default List<Warehouse> getAll() {
        return findAll();
    }

    @Override
    @NotNull
    default Optional<Warehouse> getById(@NotNull UUID id) {
        return findById(id);
    }

    @Override
    @Transactional
    @NotNull
    default Warehouse update(@NotNull Warehouse warehouse) {
        return save(warehouse);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
