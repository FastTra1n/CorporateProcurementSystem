package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, UserRole role, Instant createdAt,
                           boolean enabled, boolean mustChangePassword) {
    public static UserResponse of(User user, boolean mustChangePassword) {
        return new UserResponse(user.getId(), user.getUsername(), user.getUserRole(),
                user.getCreatedAt(), user.isEnabled(), mustChangePassword);
    }
}
