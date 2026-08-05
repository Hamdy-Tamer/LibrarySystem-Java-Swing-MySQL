package model;

import java.time.LocalDate;

public class Book {

    private int id;
    private String name;
    private BookCategory category;
    private boolean borrowed;
    private LocalDate borrowingDate;
    private int borrowingPeriod;
    private LocalDate returnDate;

    // Constructor with Enum
    public Book(String name, BookCategory category) {
        this.name = name;
        this.category = category;
        this.borrowed = false;
    }

    // Convenience constructor with String (converts automatically)
    public Book(String name, String category) {
        this(name, BookCategory.fromString(category));
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BookCategory getCategory() {
        return category;
    }

    // Get category as display name string (for database storage)
    public String getCategoryDisplayName() {
        return category.getDisplayName();
    }

    // Get category as enum name string (for database storage alternative)
    public String getCategoryEnumName() {
        return category.name();
    }

    public void setCategory(BookCategory category) {
        this.category = category;
    }

    public void setCategory(String category) {
        this.category = BookCategory.fromString(category);
    }

    public boolean getBorrowed() {
        return borrowed;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }

    public LocalDate getBorrowingDate() {
        return borrowingDate;
    }

    public void setBorrowingDate(LocalDate borrowingDate) {
        this.borrowingDate = borrowingDate;
    }

    public int getBorrowingPeriod() {
        return borrowingPeriod;
    }

    public void setBorrowingPeriod(int borrowingPeriod) {
        this.borrowingPeriod = borrowingPeriod;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void clearBorrowingDetails() {
        borrowingDate = null;
        returnDate = null;
        borrowingPeriod = 0;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category.getDisplayName() +
                ", borrowed=" + borrowed +
                '}';
    }
}
