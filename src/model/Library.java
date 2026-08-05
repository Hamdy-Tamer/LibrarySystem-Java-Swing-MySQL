package model;

import db.DBConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Library {

    // Get categories as enum array
    public static BookCategory[] getCategories() {
        return BookCategory.values();
    }

    // Get categories as display name strings
    public static String[] getCategoryDisplayNames() {
        return BookCategory.getDisplayNames();
    }

    public Library() {
        // No in‑memory initialisation – all data is read from the DB on demand.
    }

    // ---------- Add a new book ----------
    public int addBook(Book book) {
        String sql = "INSERT INTO books (name, category, borrowed, borrowing_date, borrowing_period, return_date) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getCategoryDisplayName()); // Store display name
            pstmt.setBoolean(3, book.getBorrowed());
            pstmt.setDate(4, book.getBorrowingDate() != null ? Date.valueOf(book.getBorrowingDate()) : null);
            pstmt.setInt(5, book.getBorrowingPeriod());
            pstmt.setDate(6, book.getReturnDate() != null ? Date.valueOf(book.getReturnDate()) : null);

            int affected = pstmt.executeUpdate();
            if (affected == 0) return -1;

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    book.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ---------- Remove a single book (only if not borrowed) ----------
    public boolean removeBook(int id) {
        String sql = "DELETE FROM books WHERE id = ? AND borrowed = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------- Delete all copies of a title (group) ----------
    public int deleteGroup(String name, String category) {
        String sql = "DELETE FROM books WHERE name = ? AND category = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // ---------- Borrow a book (max 15 days) ----------
    public boolean borrowBook(int id, int borrowingPeriod) {
        if (borrowingPeriod > 15) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate returnDate = today.plusDays(borrowingPeriod);
        String sql = "UPDATE books SET borrowed = TRUE, borrowing_date = ?, borrowing_period = ?, return_date = ? "
                + "WHERE id = ? AND borrowed = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(today));
            pstmt.setInt(2, borrowingPeriod);
            pstmt.setDate(3, Date.valueOf(returnDate));
            pstmt.setInt(4, id);

            if (pstmt.executeUpdate() > 0) {
                Book book = findBook(id);
                if (book != null) {
                    book.setBorrowed(true);
                    book.setBorrowingDate(today);
                    book.setBorrowingPeriod(borrowingPeriod);
                    book.setReturnDate(returnDate);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------- Return a book ----------
    public boolean returnBook(int id) {
        String sql = "UPDATE books SET borrowed = FALSE, borrowing_date = NULL, borrowing_period = NULL, return_date = NULL "
                + "WHERE id = ? AND borrowed = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            if (pstmt.executeUpdate() > 0) {
                Book book = findBook(id);
                if (book != null) {
                    book.setBorrowed(false);
                    book.clearBorrowingDetails();
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------- Find a book by ID ----------
    public Book findBook(int id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------- Update group (title + category) for all copies ----------
    public boolean updateGroup(String oldName, String oldCategory, String newName, String newCategory) {
        String sql = "UPDATE books SET name = ?, category = ? WHERE name = ? AND category = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newCategory);
            pstmt.setString(3, oldName);
            pstmt.setString(4, oldCategory);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Get summary ----------
    public String getSummary() {
        int totalBooks = 0, borrowedCount = 0;
        Map<String, Integer> catCounts = new LinkedHashMap<>();

        // Initialize with all categories
        for (BookCategory cat : BookCategory.values()) {
            catCounts.put(cat.getDisplayName(), 0);
        }

        String sql = "SELECT category, COUNT(*) as cnt, SUM(borrowed) as borrowed_sum FROM books GROUP BY category";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String cat = rs.getString("category");
                int total = rs.getInt("cnt");
                int borrowed = rs.getInt("borrowed_sum");
                catCounts.put(cat, total);
                totalBooks += total;
                borrowedCount += borrowed;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Number of Books: ").append(totalBooks).append("    ");
        summary.append("Borrowed: ").append(borrowedCount).append("\n");
        for (Map.Entry<String, Integer> entry : catCounts.entrySet()) {
            summary.append(entry.getKey()).append(": ").append(entry.getValue()).append("    ");
        }
        return summary.toString().trim();
    }

    // ---------- Get category counts ----------
    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BookCategory cat : BookCategory.values()) {
            counts.put(cat.getDisplayName(), 0);
        }
        String sql = "SELECT category, COUNT(*) as cnt FROM books GROUP BY category";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getString("category"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    // ---------- Get all books ----------
    public List<Book> getBooks() {
        List<Book> allBooks = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                allBooks.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allBooks;
    }

    // ---------- Get groups ----------
    public List<BookGroup> getGroups() {
        Map<String, BookGroup> groupMap = new LinkedHashMap<>();
        String sql = "SELECT * FROM books ORDER BY name, category";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                String key = book.getName() + "|" + book.getCategoryDisplayName();
                BookGroup group = groupMap.get(key);
                if (group == null) {
                    group = new BookGroup(book.getName(), book.getCategory());
                    groupMap.put(key, group);
                }
                group.getCopies().add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(groupMap.values());
    }

    // ---------- Helper: map ResultSet to Book ----------
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        String categoryStr = rs.getString("category");
        BookCategory category = BookCategory.fromString(categoryStr);

        Book book = new Book(rs.getString("name"), category);
        book.setId(rs.getInt("id"));
        book.setBorrowed(rs.getBoolean("borrowed"));

        Date bDate = rs.getDate("borrowing_date");
        if (bDate != null) book.setBorrowingDate(bDate.toLocalDate());

        book.setBorrowingPeriod(rs.getInt("borrowing_period"));

        Date rDate = rs.getDate("return_date");
        if (rDate != null) book.setReturnDate(rDate.toLocalDate());

        return book;
    }
}
