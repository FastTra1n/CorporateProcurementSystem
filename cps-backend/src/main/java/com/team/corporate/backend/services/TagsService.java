package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.TagResponse;
import com.team.corporate.backend.repositories.SpringProductsRepository;
import com.team.corporate.backend.repositories.SpringTagsRepository;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.entities.Tag;
import com.team.corporate.core.exceptions.ProductNotFoundException;
import com.team.corporate.core.exceptions.TagNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TagsService {
    private final SpringTagsRepository tags;
    private final SpringProductsRepository products;
    private final Clock clock;

    public TagsService(SpringTagsRepository tags, SpringProductsRepository products, Clock clock) {
        this.tags = tags;
        this.products = products;
        this.clock = clock;
    }

    public List<TagResponse> findAll() {
        return tags.findAll().stream()
                .sorted(Comparator.comparing(Tag::getName).thenComparing(Tag::getId))
                .map(TagResponse::of)
                .toList();
    }

    public TagResponse getById(UUID id) {
        return TagResponse.of(tags.findById(id)
                .orElseThrow(() -> new TagNotFoundException("Тег не найден.")));
    }

    @Transactional
    public TagResponse create(String name) {
        return TagResponse.of(tags.save(new Tag(name, clock.instant().truncatedTo(ChronoUnit.MICROS))));
    }

    @Transactional
    public TagResponse update(UUID id, String name) {
        var tag = requireTagForUpdate(id);
        tag.setName(name);
        return TagResponse.of(tag);
    }

    @Transactional
    public void delete(UUID id) {
        tags.findByIdForUpdate(id).ifPresent(tags::delete);
    }

    public List<TagResponse> getProductTags(UUID productId) {
        var product = products.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден."));
        return product.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName).thenComparing(Tag::getId))
                .map(TagResponse::of)
                .toList();
    }

    @Transactional
    public void addToProduct(UUID productId, UUID tagId) {
        var product = requireProductForUpdate(productId);
        product.addTag(requireTagForUpdate(tagId));
    }

    @Transactional
    public void removeFromProduct(UUID productId, UUID tagId) {
        var product = requireProductForUpdate(productId);
        product.removeTag(requireTagForUpdate(tagId));
    }

    private Product requireProductForUpdate(UUID id) {
        return products.getByIdForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден."));
    }

    private Tag requireTagForUpdate(UUID id) {
        return tags.findByIdForUpdate(id)
                .orElseThrow(() -> new TagNotFoundException("Тег не найден."));
    }
}
