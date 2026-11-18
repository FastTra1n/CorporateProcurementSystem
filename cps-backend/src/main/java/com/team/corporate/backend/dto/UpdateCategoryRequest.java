package com.team.corporate.backend.dto;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 255) String name
) { }
