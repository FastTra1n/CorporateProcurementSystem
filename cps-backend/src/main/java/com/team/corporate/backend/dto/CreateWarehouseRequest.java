package com.team.corporate.backend.dto;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotBlank @Size(max = EntityValidation.TEXT_MAX_LENGTH) String name,
        @Size(max = EntityValidation.TEXT_MAX_LENGTH) String location
) {}
