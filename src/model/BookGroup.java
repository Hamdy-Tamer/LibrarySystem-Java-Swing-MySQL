package model;

import java.util.ArrayList;
import java.util.List;

public class BookGroup {

    private final String name;
    private final String category;
    private final List<Book> copies;

    public BookGroup(String name, String category) {
        this.name = name;
        this.category = category;
        this.copies = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
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
}