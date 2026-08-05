package model;

public enum BookCategory {
    BIOLOGY("Biology"),
    MATHS("Maths"),
    HISTORY("History"),
    CHEMISTRY("Chemistry"),
    POLITICS("Politics");

    private final String displayName;

    BookCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Convert from String to Enum (case insensitive)
    public static BookCategory fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        // Try matching display name first (case insensitive)
        for (BookCategory category : BookCategory.values()) {
            if (category.getDisplayName().equalsIgnoreCase(text.trim())) {
                return category;
            }
        }

        // Try matching enum name (e.g., "HISTORY")
        try {
            return BookCategory.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: '" + text +
                    "'. Must be one of: " + String.join(", ", getDisplayNames()));
        }
    }

    // Get all display names as String array (for JComboBox)
    public static String[] getDisplayNames() {
        BookCategory[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].getDisplayName();
        }
        return names;
    }

    // Get all enum names as String array
    public static String[] getNames() {
        BookCategory[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].name();
        }
        return names;
    }

    @Override
    public String toString() {
        return displayName;
    }
}