package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@SoftDelete(
        strategy = SoftDeleteType.TIMESTAMP,
        columnName = "deleted_at"
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id") ,
            inverseJoinColumns = @JoinColumn(name = "tags_id")

    )
    private List<Tag> tags;

    @Version
    private Long version;

    public Product() {
    }

    public Product(@NotNull String sku,
                   @NotNull String name,
                   @Nullable Category category,
                   @NotNull BigDecimal price) {
        this.sku = EntityValidation.requireText(sku, "Артикул");
        this.name = EntityValidation.requireText(name, "Название товара");
        this.category = category;
        this.price = EntityValidation.requireMoney(price, "Цена");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getSku() {
        return sku;
    }

    public void setSku(@NotNull String sku) {
        this.sku = EntityValidation.requireText(sku, "Артикул");
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = EntityValidation.requireText(name, "Название товара");
    }

    @Nullable
    public Category getCategory() {
        return category;
    }

    public void setCategory(@Nullable Category category) {
        this.category = category;
    }

    @NotNull
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@NotNull BigDecimal price) {
        this.price = EntityValidation.requireMoney(price, "Цена");
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireText(sku, "Артикул");
        EntityValidation.requireText(name, "Название товара");
        EntityValidation.requireMoney(price, "Цена");
    }
}
