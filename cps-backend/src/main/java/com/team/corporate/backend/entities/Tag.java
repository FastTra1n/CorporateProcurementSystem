package com.team.corporate.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tags")
@SoftDelete(
        strategy = SoftDeleteType.TIMESTAMP,
        columnName = "deleted_at"
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Tag() {
    }

    public Tag(@NotNull String name, @NotNull Instant createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    @NotNull
    public UUID getId() { return id; }

    @NotNull
    public String getName() { return name; }

    @NotNull
    public Instant getCreatedAt() { return createdAt; }

}
