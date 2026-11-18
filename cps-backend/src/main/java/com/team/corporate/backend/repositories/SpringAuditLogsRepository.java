package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.repositories.AuditLogsRepository;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringAuditLogsRepository extends AuditLogsRepository, ListCrudRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    String SELECT_QUERY = "SELECT a FROM AuditLog a JOIN FETCH a.order o JOIN FETCH o.user JOIN FETCH a.changedBy";

    @Override
    @Transactional
    @NotNull
    default AuditLog add(@NotNull AuditLog auditLog) {
        return save(auditLog);
    }

    @Override
    @Query(SELECT_QUERY)
    @NotNull
    List<AuditLog> getAll();

    @Override
    @EntityGraph(attributePaths = {"order.user", "changedBy"})
    @NonNull
    Page<AuditLog> findAll(@NonNull Specification<AuditLog> spec, @NonNull Pageable page);

    @Override
    @Query(SELECT_QUERY + " WHERE a.id = :id")
    @NotNull
    Optional<AuditLog> getById(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE a.order.id = :id")
    @NotNull
    List<AuditLog> getByOrderId(@NotNull @Param("id") UUID id);

    @Override
    @Query(SELECT_QUERY + " WHERE a.changedBy.id = :id")
    @NotNull
    List<AuditLog> getByUserId(@NotNull @Param("id") UUID id);

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
