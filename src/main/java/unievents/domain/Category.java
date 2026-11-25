package unievents.domain;

import unievents.shared.DomainException;

public class Category {
    private final int categoryId;
    private final String name;

    public Category(int categoryId, String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Category name cannot be empty.");
        }
        this.categoryId = categoryId;
        this.name = name.trim();
    }

    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
