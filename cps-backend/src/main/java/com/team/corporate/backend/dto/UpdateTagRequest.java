package com.team.corporate.backend.dto;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @NotBlank @Size(max = EntityValidation.TEXT_MAX_LENGTH) String name
) {}
