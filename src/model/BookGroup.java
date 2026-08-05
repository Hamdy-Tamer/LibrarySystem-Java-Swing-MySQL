package model;

import java.util.ArrayList;
import java.util.List;

public class BookGroup {

    private final String name;
    private final BookCategory category;
    private final List<Book> copies;

    public BookGroup(String name, BookCategory category) {
        this.name = name;
        this.category = category;
        this.copies = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public BookCategory getCategory() {
        return category;
    }

    public String getCategoryDisplayName() {
        return category.getDisplayName();
    }

    public List<Book> getCopies() {
        return copies;
    }

    public int total() {
        return copies.size();
    }

    public int available() {
        int count = 0;
        for (Book book : copies) {
            if (!book.getBorrowed()) {
                count++;
            }
        }
        return count;
    }

    public int borrowedCount() {
        return total() - available();
    }

    @Override
    public String toString() {
        return "BookGroup{" +
                "name='" + name + '\'' +
                ", category=" + category.getDisplayName() +
                ", copies=" + copies.size() +
                '}';
    }
}
