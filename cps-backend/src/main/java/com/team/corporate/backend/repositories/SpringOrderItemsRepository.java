package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.repositories.OrderItemsRepository;
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
public interface SpringOrderItemsRepository extends OrderItemsRepository, ListCrudRepository<OrderItem, UUID> {
    String SELECT_QUERY = "SELECT i FROM OrderItem i JOIN FETCH i.order o JOIN FETCH o.user " +
            "JOIN FETCH i.product p LEFT JOIN FETCH p.category";

    @Override
    @Transactional
    @NotNull
    default OrderItem add(@NotNull OrderItem orderItem) {
        return save(orderItem);
    }

    @Override
    @Transactional
    @NotNull
    default List<OrderItem> addAll(@NotNull List<OrderItem> orderItems) {
        return saveAll(List.copyOf(orderItems));
    }

    @Override
    @Query(SELECT_QUERY)
    @NotNull
    List<OrderItem> getAll();

    @Override
    @Query(SELECT_QUERY + " WHERE i.id = :id")
    @NotNull
    Optional<OrderItem> getById(@NotNull @Param("id") UUID id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT i FROM OrderItem i WHERE i.id = :id")
    @NotNull
    Optional<OrderItem> getByIdForUpdate(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE i.order.id = :id")
    @NotNull
    List<OrderItem> getByOrderId(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE i.product.id = :id")
    @NotNull
    List<OrderItem> getByProductId(@NotNull @Param("id") UUID id);

    @Override
    @Transactional
    @NotNull
    default OrderItem update(@NotNull OrderItem orderItem) {
        return save(orderItem);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
