package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Size(max = 255) String fullName,
        @NotNull UserRole role,
        @NotNull Boolean enabled
) {
}
