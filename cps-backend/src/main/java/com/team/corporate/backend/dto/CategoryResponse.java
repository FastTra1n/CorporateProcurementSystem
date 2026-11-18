package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {
    public static CategoryResponse of(Category category){
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}
