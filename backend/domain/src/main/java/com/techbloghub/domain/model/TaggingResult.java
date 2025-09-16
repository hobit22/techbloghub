package com.techbloghub.domain.model;

import java.util.List;

public record TaggingResult(
        List<String> tags,
        List<String> categories,
        List<String> rejectedTags,
        List<String> rejectedCategories
) {
    public TaggingResult(List<String> tags, List<String> categories) {
        this(tags, categories, List.of(), List.of());
    }

    public boolean isEmpty() {
        return tags.isEmpty() && categories.isEmpty();
    }

    public boolean hasRejectedItems() {
        return !rejectedTags.isEmpty() || !rejectedCategories.isEmpty();
    }
}
