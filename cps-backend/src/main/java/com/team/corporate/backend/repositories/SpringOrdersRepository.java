package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.repositories.OrdersRepository;
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
public interface SpringOrdersRepository extends OrdersRepository, ListCrudRepository<Order, UUID> {
    String SELECT_QUERY = "SELECT o FROM Order o JOIN FETCH o.user";

    @Override
    @Transactional
    @NotNull
    default Order add(@NotNull Order order) {
        return save(order);
    }

    @Override
    @Query(SELECT_QUERY)
    @NotNull
    List<Order> getAll();

    @Override
    @Query(SELECT_QUERY + " WHERE o.id = :id")
    @NotNull
    Optional<Order> getById(@NotNull @Param("id") UUID id);

    @Override
    @Transactional
    @NotNull
    default Order update(@NotNull Order order) {
        return save(order);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    @NotNull
    Optional<Order> getByIdForUpdate(@NotNull @Param("id") UUID id);

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
