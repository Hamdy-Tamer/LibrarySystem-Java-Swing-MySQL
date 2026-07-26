package view;

import auth.Login;
import model.Book;
import model.BookGroup;
import model.Library;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class UserDashboard extends JFrame {

    private enum FilterMode { AVAILABLE, BORROWED }

    private Library library;
    private User loggedUser;
    private JTabbedPane tabbedPane;
    private List<BookTabPanel> tabPanels = new ArrayList<>();
    private JPanel historyPanel;

    public UserDashboard(User user) {
        super("Library - User Dashboard");
        this.loggedUser = user;
        library = new Library();

        setSize(900, 650);
        setLocationRelativeTo(null);

        ImageIcon icon = loadIcon("images/Library.jpeg");
        if (icon != null) setIconImage(icon.getImage());

        initComponents();
        updateStatus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(UserDashboard.this,
                        "Are you sure you want to exit?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        loadIcon("images/exit-icon.jpg"));
                if (confirm == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Top Panel: Welcome message (left) + Logout button (right)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Welcome message (left)
        JLabel welcomeLabel = new JLabel("Welcome, " + loggedUser.getFirstName() + " " + loggedUser.getLastName() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 15));
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        // Logout button (right)
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        topPanel.add(logoutBtn, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: Tabs only (no status panel)
        mainPanel.add(createTabs(), BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                loadIcon("images/exit-icon.jpg"));
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            Login login = new Login(null);
            login.setVisible(true);
        }
    }

    private JTabbedPane createTabs() {
        tabbedPane = new JTabbedPane();

        // Available tab (with Borrow)
        BookTabPanel availableTab = new BookTabPanel(FilterMode.AVAILABLE, true, false);
        tabPanels.add(availableTab);
        tabbedPane.addTab("Available Books", availableTab.panel);

        // Borrowed tab (with Return) - ONLY SHOWS USER'S OWN BORROWED BOOKS
        BookTabPanel borrowedTab = new BookTabPanel(FilterMode.BORROWED, false, true);
        tabPanels.add(borrowedTab);
        tabbedPane.addTab("My Borrowed Books", borrowedTab.panel);

        // My History tab
        tabbedPane.addTab("My History", createHistoryPanel());

        return tabbedPane;
    }

    // ---------- History Panel (User's own borrowing history) ----------
    private JPanel createHistoryPanel() {
        historyPanel = new JPanel(new BorderLayout(10, 10));
        historyPanel.setBorder(BorderFactory.createTitledBorder("My Borrowing History"));

        String[] columns = {"Borrowing ID", "Book ID", "Book Name", "Borrow Date", "Return Date", "Status", "Fine"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadHistory(historyModel));
        buttonPanel.add(refreshBtn);
        historyPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        loadHistory(historyModel);

        return historyPanel;
    }

    // Returns the fine that should be DISPLAYED after delaying on retuning book
    private double computeDisplayFine(String status, java.sql.Date dueDate, double storedFine) {
        if (!"borrowed".equals(status) || dueDate == null) {
            return storedFine;
        }
        long daysLate = ChronoUnit.DAYS.between(dueDate.toLocalDate(), LocalDate.now());
        return daysLate > 0 ? daysLate * LATE_FEE_PER_DAY : 0.0;
    }

    private String computeDisplayStatus(String status, java.sql.Date dueDate) {
        if ("borrowed".equals(status) && dueDate != null && dueDate.toLocalDate().isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return status;
    }

    private void loadHistory(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT b.borrowing_id, b.book_id, bk.name, b.borrowing_date, b.return_date, b.status, b.fine_amount " +
                "FROM borrowings b JOIN books bk ON b.book_id = bk.id WHERE b.user_id = ? ORDER BY b.borrowing_date DESC";
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loggedUser.getUserId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String status = rs.getString("status");
                java.sql.Date dueDate = rs.getDate("return_date");
                double fine = computeDisplayFine(status, dueDate, rs.getDouble("fine_amount"));
                model.addRow(new Object[]{
                        rs.getInt("borrowing_id"),
                        rs.getInt("book_id"),
                        rs.getString("name"),
                        rs.getDate("borrowing_date"),
                        dueDate,
                        computeDisplayStatus(status, dueDate),
                        fine
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load history: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- BookTabPanel with user-specific filtering ----------
    private class BookTabPanel {
        final FilterMode mode;
        final JPanel panel;
        final JTable groupTable;
        final DefaultTableModel groupTableModel;
        final TableRowSorter<DefaultTableModel> groupSorter;
        JTable copyTable;
        DefaultTableModel copyTableModel;
        BookGroup selectedGroup;
        final JButton borrowBtn;
        final JButton returnBtn;

        BookTabPanel(FilterMode mode, boolean showBorrow, boolean showReturn) {
            this.mode = mode;
            this.panel = new JPanel(new BorderLayout(5, 5));

            // Search bar
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.add(new JLabel("Search:"));
            JTextField searchField = new JTextField(20);
            searchPanel.add(searchField);
            panel.add(searchPanel, BorderLayout.NORTH);

            // Group table
            String countLabel = (mode == FilterMode.BORROWED) ? "My Borrowed / Total" : "Available / Total";
            String[] groupColumns = {"Title", "Category", countLabel, "Copy IDs"};
            groupTableModel = new DefaultTableModel(groupColumns, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            groupTable = new JTable(groupTableModel);
            groupTable.setRowHeight(24);
            groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            groupSorter = new TableRowSorter<>(groupTableModel);
            groupTable.setRowSorter(groupSorter);
            groupTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) onGroupSelected();
            });

            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
                private void filter() {
                    String text = searchField.getText().trim();
                    groupSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
                }
            });

            JScrollPane groupScrollPane = new JScrollPane(groupTable);
            groupScrollPane.setBorder(BorderFactory.createTitledBorder("Titles"));

            // Copy table
            String[] copyColumns = {"ID", "Status", "Borrow Date", "Return Date"};
            copyTableModel = new DefaultTableModel(copyColumns, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            copyTable = new JTable(copyTableModel);
            copyTable.setRowHeight(24);
            copyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JScrollPane copyScrollPane = new JScrollPane(copyTable);
            copyScrollPane.setBorder(BorderFactory.createTitledBorder("Copies of Selected Title"));

            // Buttons Panel
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

            borrowBtn = new JButton("Borrow Selected Copy");
            returnBtn = new JButton("Return Selected Copy");

            borrowBtn.addActionListener(e -> borrowSelectedCopy(this));
            returnBtn.addActionListener(e -> returnSelectedCopy(this));

            if (showBorrow) buttonsPanel.add(borrowBtn);
            if (showReturn) buttonsPanel.add(returnBtn);

            JButton refreshBtn = new JButton("Refresh");
            refreshBtn.addActionListener(e -> {
                refreshGroupTable();
                refreshCopyTable();
            });
            buttonsPanel.add(refreshBtn);

            JPanel copyPanel = new JPanel(new BorderLayout(5, 5));
            copyPanel.add(copyScrollPane, BorderLayout.CENTER);
            copyPanel.add(buttonsPanel, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, groupScrollPane, copyPanel);
            splitPane.setResizeWeight(0.5);
            panel.add(splitPane, BorderLayout.CENTER);
        }

        // ---------- Only return groups that have copies borrowed by the logged-in user ----------
        List<BookGroup> filteredGroups() {
            List<BookGroup> result = new ArrayList<>();
            for (BookGroup g : library.getGroups()) {
                switch (mode) {
                    case AVAILABLE:
                        if (g.available() > 0) result.add(g);
                        break;
                    case BORROWED:
                        // Check if this group has ANY copy borrowed by the logged-in user
                        for (Book book : g.getCopies()) {
                            if (book.getBorrowed()) {
                                if (isBookBorrowedByUser(book.getId())) {
                                    result.add(g);
                                    break;
                                }
                            }
                        }
                        break;
                }
            }
            return result;
        }

        // ---------- Only return copies borrowed by the logged-in user ----------
        List<Book> filteredCopies(BookGroup group) {
            List<Book> result = new ArrayList<>();
            if (group == null) return result;
            for (Book b : group.getCopies()) {
                switch (mode) {
                    case AVAILABLE:
                        if (!b.getBorrowed()) result.add(b);
                        break;
                    case BORROWED:
                        if (b.getBorrowed() && isBookBorrowedByUser(b.getId())) {
                            result.add(b);
                        }
                        break;
                }
            }
            return result;
        }

        // ---------- Helper: Check if a book is borrowed by the logged-in user ----------
        private boolean isBookBorrowedByUser(int bookId) {
            String sql = "SELECT borrowing_id FROM borrowings WHERE book_id = ? AND user_id = ? AND status = 'borrowed' LIMIT 1";
            try (Connection conn = db.DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                pstmt.setInt(2, loggedUser.getUserId());
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }

        void refreshGroupTable() {
            groupTableModel.setRowCount(0);
            for (BookGroup g : filteredGroups()) {
                String countText;
                if (mode == FilterMode.BORROWED) {
                    int userBorrowedCount = 0;
                    for (Book book : g.getCopies()) {
                        if (book.getBorrowed() && isBookBorrowedByUser(book.getId())) {
                            userBorrowedCount++;
                        }
                    }
                    countText = userBorrowedCount + " / " + g.total();
                } else {
                    countText = g.available() + " / " + g.total();
                }
                groupTableModel.addRow(new Object[]{g.getName(), g.getCategory(), countText, idsToString(g)});
            }
        }

        void refreshCopyTable() {
            copyTableModel.setRowCount(0);
            if (selectedGroup == null) return;
            for (Book book : filteredCopies(selectedGroup)) {
                String status = book.getBorrowed() ? "Borrowed" : "Available";
                String borrowDate = book.getBorrowed() ? String.valueOf(book.getBorrowingDate()) : "";
                String returnDate = book.getBorrowed() ? String.valueOf(book.getReturnDate()) : "";
                copyTableModel.addRow(new Object[]{book.getId(), status, borrowDate, returnDate});
            }
        }

        void onGroupSelected() {
            int viewRow = groupTable.getSelectedRow();
            if (viewRow < 0) {
                selectedGroup = null;
                copyTableModel.setRowCount(0);
                return;
            }
            int modelRow = groupTable.convertRowIndexToModel(viewRow);
            String name = (String) groupTableModel.getValueAt(modelRow, 0);
            String category = (String) groupTableModel.getValueAt(modelRow, 1);
            selectedGroup = findGroup(name, category);
            refreshCopyTable();
        }

        void trySelect(String name, String category) {
            for (int row = 0; row < groupTableModel.getRowCount(); row++) {
                if (groupTableModel.getValueAt(row, 0).equals(name)
                        && groupTableModel.getValueAt(row, 1).equals(category)) {
                    int viewRow = groupTable.convertRowIndexToView(row);
                    if (viewRow >= 0) {
                        groupTable.setRowSelectionInterval(viewRow, viewRow);
                        groupTable.scrollRectToVisible(groupTable.getCellRect(viewRow, 0, true));
                    }
                    return;
                }
            }
            selectedGroup = null;
            copyTableModel.setRowCount(0);
        }

        Integer getSelectedCopyId() {
            int row = copyTable.getSelectedRow();
            if (row < 0) {
                showError("Please select a copy from the table first.");
                return null;
            }
            return (Integer) copyTableModel.getValueAt(row, 0);
        }
    }

    // ---------- Helper Methods ----------
    private BookGroup findGroup(String name, String category) {
        for (BookGroup group : library.getGroups()) {
            if (group.getName().equals(name) && group.getCategory().equals(category)) {
                return group;
            }
        }
        return null;
    }

    private String idsToString(BookGroup group) {
        StringBuilder sb = new StringBuilder();
        for (Book b : group.getCopies()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(b.getId());
        }
        return sb.toString();
    }

    private void borrowSelectedCopy(BookTabPanel tab) {
        Integer id = tab.getSelectedCopyId();
        if (id == null) return;
        Book book = library.findBook(id);
        if (book == null) return;
        if (book.getBorrowed()) {
            showError("This copy is already borrowed.");
            return;
        }
        String periodInput = JOptionPane.showInputDialog(this, "Enter Borrowing Period (days, max 15):");
        if (periodInput == null) return;
        try {
            int period = Integer.parseInt(periodInput.trim());
            if (period <= 0 || period > 15) {
                showError("Period must be between 1 and 15 days.");
                return;
            }
            if (library.borrowBook(id, period)) {
                addBorrowingHistory(id, period);
                book = library.findBook(id);
                String name = book.getName(), category = book.getCategory();
                updateStatus();
                selectGroupInAllTabs(name, category);
                tab.refreshCopyTable();
                showSuccess("Borrowed copy #" + id + ". Return Date: " + book.getReturnDate());
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number.");
        }
    }

    private void addBorrowingHistory(int bookId, int period) {
        String sql = "INSERT INTO borrowings (user_id, book_id, borrowing_date, return_date, borrowing_period, status) " +
                "VALUES (?, ?, ?, ?, ?, 'borrowed')";
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loggedUser.getUserId());
            pstmt.setInt(2, bookId);
            pstmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now().plusDays(period)));
            pstmt.setInt(5, period);
            pstmt.executeUpdate();

            // Update total_books_borrowed in members table
            String updateMemberSql = "UPDATE members SET total_books_borrowed = total_books_borrowed + 1 WHERE user_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateMemberSql)) {
                updateStmt.setInt(1, loggedUser.getUserId());
                updateStmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Error recording borrowing: " + ex.getMessage());
        }
    }

    private void returnSelectedCopy(BookTabPanel tab) {
        Integer id = tab.getSelectedCopyId();
        if (id == null) return;
        Book book = library.findBook(id);
        if (book == null) return;
        if (!book.getBorrowed()) {
            showError("This copy is not currently borrowed.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Return copy #" + id + "?",
                "Confirm Return", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String name = book.getName(), category = book.getCategory();
            if (library.returnBook(id)) {
                updateBorrowingHistory(id);
                updateStatus();
                selectGroupInAllTabs(name, category);
                tab.refreshCopyTable();
                if (lastReturnFine > 0) {
                    showSuccess("Copy #" + id + " has been returned.\n" +
                            "This return was late. A fine of $" + String.format("%.2f", lastReturnFine) +
                            " has been added to your account.");
                } else {
                    showSuccess("Copy #" + id + " has been returned.");
                }
            }
        }
    }

    private static final double LATE_FEE_PER_DAY = 15.0;

    private void updateBorrowingHistory(int bookId) {
        String selectSql = "SELECT borrowing_id, return_date FROM borrowings " +
                "WHERE book_id = ? AND user_id = ? AND status = 'borrowed' ORDER BY borrowing_id DESC LIMIT 1";
        String updateSql = "UPDATE borrowings SET actual_return_date = ?, status = 'returned', fine_amount = ? " +
                "WHERE borrowing_id = ?";
        String updateMemberFineSql = "UPDATE members SET current_fines = current_fines + ? WHERE user_id = ?";

        LocalDate today = LocalDate.now();

        try (Connection conn = db.DBConnection.getConnection()) {
            int borrowingId = -1;
            LocalDate dueDate = null;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, bookId);
                selectStmt.setInt(2, loggedUser.getUserId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        borrowingId = rs.getInt("borrowing_id");
                        dueDate = rs.getDate("return_date").toLocalDate();
                    }
                }
            }

            if (borrowingId == -1) return; // no matching active borrowing found

            long daysLate = ChronoUnit.DAYS.between(dueDate, today);
            double fine = daysLate > 0 ? daysLate * LATE_FEE_PER_DAY : 0.0;

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDate(1, java.sql.Date.valueOf(today));
                updateStmt.setDouble(2, fine);
                updateStmt.setInt(3, borrowingId);
                updateStmt.executeUpdate();
            }

            if (fine > 0) {
                try (PreparedStatement memberStmt = conn.prepareStatement(updateMemberFineSql)) {
                    memberStmt.setDouble(1, fine);
                    memberStmt.setInt(2, loggedUser.getUserId());
                    memberStmt.executeUpdate();
                }
                lastReturnFine = fine;
            } else {
                lastReturnFine = 0.0;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private double lastReturnFine = 0.0;

    private void updateStatus() {
        for (BookTabPanel tab : tabPanels) {
            tab.refreshGroupTable();
            tab.refreshCopyTable();
        }
        // Refresh history tab if visible
        Component selected = tabbedPane.getSelectedComponent();
        if (selected == historyPanel) {
            JTable table = (JTable) ((JScrollPane) ((JPanel) selected).getComponent(0)).getViewport().getView();
            loadHistory((DefaultTableModel) table.getModel());
        }
    }

    private void selectGroupInAllTabs(String name, String category) {
        for (BookTabPanel tab : tabPanels) {
            tab.trySelect(name, category);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE, loadIcon("images/error.jpg"));
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE, loadIcon("images/success.jpg"));
    }

    private ImageIcon loadIcon(String path) {
        File file = new File(path);
        if (file.exists()) return new ImageIcon(path);
        return null;
    }
}