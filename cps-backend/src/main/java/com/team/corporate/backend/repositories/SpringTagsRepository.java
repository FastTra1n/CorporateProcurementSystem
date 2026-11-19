package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.Tag;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringTagsRepository extends ListCrudRepository<Tag, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT t FROM Tag t WHERE t.id = :id")
    Optional<Tag> findByIdForUpdate(@Param("id") UUID id);
}
