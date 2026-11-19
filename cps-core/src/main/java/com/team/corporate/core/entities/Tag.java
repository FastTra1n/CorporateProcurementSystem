package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tags")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new LinkedHashSet<>();

    @Version
    private Long version;

    protected Tag() {}

    public Tag(@NotNull String name, @NotNull Instant createdAt) {
        this.name = EntityValidation.requireText(name, "Название тега");
        this.createdAt = EntityValidation.requireNonNull(createdAt, "Дата создания тега");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = EntityValidation.requireText(name, "Название тега");
    }

    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @NotNull
    public Set<Product> getProducts() {
        return products;
    }
}
