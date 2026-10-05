package com.dndmasters.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolves the authenticated user id from the security context. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Long id ? id : null;
    }

    public static Long requireUserId() {
        Long id = currentUserId();
        if (id == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return id;
    }
}
