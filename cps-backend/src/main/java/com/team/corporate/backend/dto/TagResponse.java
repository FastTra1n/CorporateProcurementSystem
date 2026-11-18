package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Tag;

import java.time.Instant;
import java.util.UUID;

public record TagResponse(UUID id, String name, Instant createdAt) {
    public static TagResponse of(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
