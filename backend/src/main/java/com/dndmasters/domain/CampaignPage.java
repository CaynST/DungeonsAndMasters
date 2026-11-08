package com.dndmasters.domain;

import java.util.List;

/**
 * Read model for a paginated campaign listing. Kept separate from the persistence
 * {@code Page} so controllers and callers do not depend on Spring Data types.
 */
public record CampaignPage(
        List<Campaign> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
