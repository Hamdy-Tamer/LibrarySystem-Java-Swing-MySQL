package model;

import java.time.LocalDate;

public class Book {

    private int id;
    private String name;
    private String category;
    private boolean borrowed;
    private LocalDate borrowingDate;
    private int borrowingPeriod;
    private LocalDate returnDate;

    public Book(String name, String category) {
        this.name = name;
        this.category = category;
        this.borrowed = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
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
}