package com.team.corporate.backend.security;

import org.springframework.security.core.Authentication;

public final class PasswordChangePolicy {
    public static final String REQUIRED_AUTHORITY = "PASSWORD_CHANGE_REQUIRED";

    private PasswordChangePolicy() {
    }

    public static boolean isRequired(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> REQUIRED_AUTHORITY.equals(authority.getAuthority()));
    }
}
