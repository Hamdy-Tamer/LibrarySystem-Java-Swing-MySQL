package view;

import model.Book;
import model.BookGroup;
import model.Library;

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

public class EmployeeDashboard extends JFrame {

    private enum FilterMode { ALL, AVAILABLE, BORROWED }

    private Library library;
    private JTextField bookNameField;
    private JComboBox<String> categoryComboBox;
    private JTextField quantityField;
    private JTextArea summaryTextArea;

    private JTabbedPane tabbedPane;
    private List<BookTabPanel> tabPanels = new ArrayList<>();
    private JPanel userManagementPanel;
    private JPanel employeeManagementPanel;

    // Table models for user and employee management
    private DefaultTableModel userTableModel;
    private DefaultTableModel employeeTableModel;

    public EmployeeDashboard() {
        super("Library Management System - Employee Dashboard");
        library = new Library();

        setSize(1000, 700);
        setLocationRelativeTo(null);

        ImageIcon icon = loadIcon("images/Library.jpeg");
        if (icon != null) setIconImage(icon.getImage());

        initComponents();
        updateStatus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(EmployeeDashboard.this,
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

        // Top: Add Book panel
        mainPanel.add(createAddBookPanel(), BorderLayout.NORTH);

        // Center: Tabbed pane with Library tabs + User Management + Employee Management
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(createStatusPanel(), BorderLayout.NORTH);
        centerPanel.add(createTabs(), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        handleMenu();
    }

    private JPanel createAddBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Add a New Book"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Book Name:"), gbc);
        gbc.gridx = 1;
        bookNameField = new JTextField(15);
        panel.add(bookNameField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 3;
        categoryComboBox = new JComboBox<>(Library.getCategories());
        panel.add(categoryComboBox, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 5;
        quantityField = new JTextField(4);
        quantityField.setText("1");
        panel.add(quantityField, gbc);

        gbc.gridx = 6;
        JButton addBookButton = new JButton("Add Book(s)");
        addBookButton.addActionListener(e -> addBook());
        panel.add(addBookButton, gbc);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Library Status"));

        summaryTextArea = new JTextArea(2, 0);
        summaryTextArea.setEditable(false);
        summaryTextArea.setLineWrap(true);
        summaryTextArea.setBackground(panel.getBackground());
        panel.add(summaryTextArea, BorderLayout.NORTH);
        return panel;
    }

    private JTabbedPane createTabs() {
        tabbedPane = new JTabbedPane();

        // Library tabs - NO Borrow/Return buttons
        BookTabPanel allTab = new BookTabPanel(FilterMode.ALL, false, false, true, true);
        tabPanels.add(allTab);
        tabbedPane.addTab("All", allTab.panel);

        BookTabPanel availableTab = new BookTabPanel(FilterMode.AVAILABLE, false, false, true, true);
        tabPanels.add(availableTab);
        tabbedPane.addTab("Available", availableTab.panel);

        BookTabPanel borrowedTab = new BookTabPanel(FilterMode.BORROWED, false, false, true, false);
        tabPanels.add(borrowedTab);
        tabbedPane.addTab("Borrowed", borrowedTab.panel);

        // User Management tab (customers)
        tabbedPane.addTab("Users", createUserManagementPanel());

        // Employee Management tab (staff)
        tabbedPane.addTab("Employees", createEmployeeManagementPanel());

        return tabbedPane;
    }

    // ---------- User Management Panel (Customers) ----------
    private JPanel createUserManagementPanel() {
        userManagementPanel = new JPanel(new BorderLayout(10, 10));
        userManagementPanel.setBorder(BorderFactory.createTitledBorder("User Management - Customers"));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Users:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        userManagementPanel.add(searchPanel, BorderLayout.NORTH);

        // User table
        String[] columns = {"User ID", "Name", "Email", "Phone", "Member Code", "Books Borrowed", "Total Fines"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable userTable = new JTable(userTableModel);
        userTable.setRowHeight(24);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> userSorter = new TableRowSorter<>(userTableModel);
        userTable.setRowSorter(userSorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                userSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
            }
        });

        JScrollPane scrollPane = new JScrollPane(userTable);
        userManagementPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("🔄 Refresh");
        JButton deleteUserBtn = new JButton("🗑️ Delete Selected User");
        JButton viewHistoryBtn = new JButton("📖 View Borrowing History");
        JButton settleFineBtn = new JButton("💲 Settle Fine");

        refreshBtn.addActionListener(e -> refreshUsers());
        deleteUserBtn.addActionListener(e -> deleteSelectedUser(userTable));
        viewHistoryBtn.addActionListener(e -> viewUserHistory(userTable));
        settleFineBtn.addActionListener(e -> settleUserFine(userTable));

        buttonPanel.add(refreshBtn);
        buttonPanel.add(deleteUserBtn);
        buttonPanel.add(viewHistoryBtn);
        buttonPanel.add(settleFineBtn);
        userManagementPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        refreshUsers();

        return userManagementPanel;
    }

    // ---------- Employee Management Panel (Staff) ----------
    private JPanel createEmployeeManagementPanel() {
        employeeManagementPanel = new JPanel(new BorderLayout(10, 10));
        employeeManagementPanel.setBorder(BorderFactory.createTitledBorder("Employee Management - Staff"));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Employees:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        employeeManagementPanel.add(searchPanel, BorderLayout.NORTH);

        // Employee table
        String[] columns = {"Employee ID", "Name", "Email", "Phone", "Position", "Employee Code", "Hire Date", "Salary"};
        employeeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable employeeTable = new JTable(employeeTableModel);
        employeeTable.setRowHeight(24);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> employeeSorter = new TableRowSorter<>(employeeTableModel);
        employeeTable.setRowSorter(employeeSorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                employeeSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
            }
        });

        JScrollPane scrollPane = new JScrollPane(employeeTable);
        employeeManagementPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("🔄 Refresh");
        JButton deleteEmployeeBtn = new JButton("🗑️ Delete Selected Employee");
        JButton addEmployeeBtn = new JButton("➕ Add New Employee");

        refreshBtn.addActionListener(e -> refreshEmployees());
        deleteEmployeeBtn.addActionListener(e -> deleteSelectedEmployee(employeeTable));
        addEmployeeBtn.addActionListener(e -> addNewEmployee());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(addEmployeeBtn);
        buttonPanel.add(deleteEmployeeBtn);
        employeeManagementPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        refreshEmployees();

        return employeeManagementPanel;
    }

    // ---------- Refresh Users (Customers) ----------
    private static final double LATE_FEE_PER_DAY = 15.0;

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

    private void refreshUsers() {
        userTableModel.setRowCount(0);
        String sql = "SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone_number, " +
                "m.member_code, m.current_fines, " +
                "(SELECT COUNT(*) FROM borrowings b WHERE b.user_id = u.user_id AND b.status = 'borrowed') AS books_borrowed " +
                "FROM users u JOIN members m ON u.user_id = m.user_id " +
                "WHERE u.role = 'user' ORDER BY u.user_id";
        // Live overdue amounts aren't stored anywhere (they only get written to fine_amount/current_fines
        // when a book is actually returned), so compute them here from any still-borrowed, past-due rows.
        String overdueSql = "SELECT user_id, return_date FROM borrowings WHERE status = 'borrowed' AND return_date < ?";
        Map<Integer, Double> overdueByUser = new HashMap<>();
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(overdueSql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("user_id");
                    long daysLate = ChronoUnit.DAYS.between(rs.getDate("return_date").toLocalDate(), LocalDate.now());
                    double overdueFine = daysLate * LATE_FEE_PER_DAY;
                    overdueByUser.merge(uid, overdueFine, Double::sum);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (Connection conn = db.DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                int userId = rs.getInt("user_id");
                double totalFines = rs.getDouble("current_fines") + overdueByUser.getOrDefault(userId, 0.0);
                userTableModel.addRow(new Object[]{
                        userId,
                        name,
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("member_code"),
                        rs.getInt("books_borrowed"),
                        totalFines
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to load users: " + ex.getMessage());
        }
    }

    // ---------- Refresh Employees (Staff) ----------
    private void refreshEmployees() {
        employeeTableModel.setRowCount(0);
        String sql = "SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone_number, " +
                "e.employee_code, e.position, e.hire_date, e.salary " +
                "FROM users u JOIN employees e ON u.user_id = e.user_id " +
                "WHERE u.role = 'employee' ORDER BY u.user_id";
        try (Connection conn = db.DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                employeeTableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        name,
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("position"),
                        rs.getString("employee_code"),
                        rs.getDate("hire_date"),
                        rs.getDouble("salary")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to load employees: " + ex.getMessage());
        }
    }

    // ---------- Delete Selected User ----------
    private void deleteSelectedUser(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select a user to delete.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int userId = (int) table.getModel().getValueAt(modelRow, 0);
        String name = (String) table.getModel().getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user: " + name + "?\nThis will also delete their borrowing history.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, loadIcon("images/delete.jpg"));
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                showSuccess("User deleted successfully.");
                refreshUsers();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to delete user: " + ex.getMessage());
        }
    }

    // ---------- Delete Selected Employee ----------
    private void deleteSelectedEmployee(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select an employee to delete.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int userId = (int) table.getModel().getValueAt(modelRow, 0);
        String name = (String) table.getModel().getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete employee: " + name + "?\nThis action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, loadIcon("images/delete.jpg"));
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                showSuccess("Employee deleted successfully.");
                refreshEmployees();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to delete employee: " + ex.getMessage());
        }
    }

    // ---------- Add New Employee ----------
    private void addNewEmployee() {
        JDialog addEmployeeDialog = new JDialog(this, "Add New Employee", true);
        addEmployeeDialog.setSize(420, 420);
        addEmployeeDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // First Name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        JTextField firstNameField = new JTextField(15);
        panel.add(firstNameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        JTextField lastNameField = new JTextField(15);
        panel.add(lastNameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        panel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Phone (11 digits):"), gbc);
        gbc.gridx = 1;
        JTextField phoneField = new JTextField(20);
        panel.add(phoneField, gbc);

        // Position
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Position:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> positionCombo = new JComboBox<>(new String[]{
                "librarian", "assistant_librarian", "library_assistant", "technician", "manager", "director", "admin"
        });
        panel.add(positionCombo, gbc);

        // Salary
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Salary:"), gbc);
        gbc.gridx = 1;
        JTextField salaryField = new JTextField(20);
        salaryField.setText("0.00");
        panel.add(salaryField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String position = (String) positionCombo.getSelectedItem();
            String salaryText = salaryField.getText().trim();
            String password = new String(passwordField.getPassword());

            // Validations
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                showError("All fields are required.");
                return;
            }
            if (!firstName.matches("[a-zA-Z]{3,15}") || !lastName.matches("[a-zA-Z]{3,15}")) {
                showError("Names must be 3-15 letters only.");
                return;
            }
            if (!phone.matches("\\d{11}")) {
                showError("Phone must be exactly 11 digits.");
                return;
            }
            if (password.length() < 6) {
                showError("Password must be at least 6 characters.");
                return;
            }

            double salary;
            try {
                salary = Double.parseDouble(salaryText);
                if (salary < 0) {
                    showError("Salary cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid salary amount.");
                return;
            }

            // Insert into database
            try (Connection conn = db.DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                // Check if email exists
                String checkSql = "SELECT email FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        showError("Email already registered.");
                        return;
                    }
                }

                // Check if phone exists
                String checkPhoneSql = "SELECT phone_number FROM users WHERE phone_number = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkPhoneSql)) {
                    checkStmt.setString(1, phone);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        showError("Phone number already registered.");
                        return;
                    }
                }

                // Insert user
                String insertUser = "INSERT INTO users (first_name, last_name, email, password, phone_number, role) " +
                        "VALUES (?, ?, ?, ?, ?, 'employee')";
                int userId;
                try (PreparedStatement pstmt = conn.prepareStatement(insertUser, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, firstName);
                    pstmt.setString(2, lastName);
                    pstmt.setString(3, email);
                    pstmt.setString(4, password);
                    pstmt.setString(5, phone);
                    pstmt.executeUpdate();
                    ResultSet keys = pstmt.getGeneratedKeys();
                    if (keys.next()) {
                        userId = keys.getInt(1);
                    } else {
                        showError("Failed to create user.");
                        return;
                    }
                }

                // Insert employee with salary
                String employeeCode = "EMP" + System.currentTimeMillis();
                String insertEmployee = "INSERT INTO employees (user_id, employee_code, position, hire_date, salary) " +
                        "VALUES (?, ?, ?, CURDATE(), ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertEmployee)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, employeeCode);
                    pstmt.setString(3, position);
                    pstmt.setDouble(4, salary);
                    pstmt.executeUpdate();
                }

                conn.commit();
                showSuccess("Employee added successfully!");
                addEmployeeDialog.dispose();
                refreshEmployees();

            } catch (SQLException ex) {
                ex.printStackTrace();
                showError("Database error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> addEmployeeDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        addEmployeeDialog.add(panel);
        addEmployeeDialog.setVisible(true);
    }

    // ---------- View User History ----------
    private void viewUserHistory(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select a user to view history.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int userId = (int) table.getModel().getValueAt(modelRow, 0);
        String name = (String) table.getModel().getValueAt(modelRow, 1);

        JDialog historyDialog = new JDialog(this, "Borrowing History - " + name, true);
        historyDialog.setSize(700, 400);
        historyDialog.setLocationRelativeTo(this);

        String[] columns = {"Borrowing ID", "Book ID", "Book Name", "Borrow Date", "Return Date", "Status", "Fine"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(historyTable);

        String sql = "SELECT b.borrowing_id, b.book_id, bk.name, b.borrowing_date, b.return_date, b.status, b.fine_amount " +
                "FROM borrowings b JOIN books bk ON b.book_id = bk.id WHERE b.user_id = ? ORDER BY b.borrowing_date DESC";
        try (Connection conn = db.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String status = rs.getString("status");
                java.sql.Date dueDate = rs.getDate("return_date");
                double fine = computeDisplayFine(status, dueDate, rs.getDouble("fine_amount"));
                historyModel.addRow(new Object[]{
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
        }

        historyDialog.add(scrollPane);
        historyDialog.setVisible(true);
    }

    // ---------- Settle Fine (mark as paid in cash at the desk) ----------
    private void settleUserFine(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select a user to settle a fine for.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int userId = (int) table.getModel().getValueAt(modelRow, 0);
        String name = (String) table.getModel().getValueAt(modelRow, 1);

        double settledFines = 0.0;
        // Any book still out and overdue keeps accruing $15/day live, so we need each
        // such row's currently-owed amount (not just the flat members.current_fines total).
        Map<Integer, Double> overdueRows = new HashMap<>(); // borrowing_id -> currently-owed amount

        String memberSql = "SELECT current_fines FROM members WHERE user_id = ?";
        String overdueSql = "SELECT borrowing_id, return_date FROM borrowings " +
                "WHERE user_id = ? AND status = 'borrowed' AND return_date < ?";

        try (Connection conn = db.DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) settledFines = rs.getDouble("current_fines");
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(overdueSql)) {
                pstmt.setInt(1, userId);
                pstmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long daysLate = ChronoUnit.DAYS.between(rs.getDate("return_date").toLocalDate(), LocalDate.now());
                        overdueRows.put(rs.getInt("borrowing_id"), daysLate * LATE_FEE_PER_DAY);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to look up fines: " + ex.getMessage());
            return;
        }

        double totalOwed = settledFines + overdueRows.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalOwed <= 0) {
            showSuccess(name + " has no outstanding fines.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                name + " owes $" + String.format("%.2f", totalOwed) + " in total.\n" +
                        "Confirm that this amount has been received (e.g. in cash) and clear it?",
                "Settle Fine", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Note: any books in `overdueRows` are still checked out — paying today's accrued
        // amount doesn't return the book. We reset each one's due date to today so tomorrow's
        // calculation starts counting fresh, instead of re-charging for the days just paid.
        String zeroMemberSql = "UPDATE members SET current_fines = 0 WHERE user_id = ?";
        String resetDueDateSql = "UPDATE borrowings SET return_date = ? WHERE borrowing_id = ?";

        try (Connection conn = db.DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(zeroMemberSql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(resetDueDateSql)) {
                for (Integer borrowingId : overdueRows.keySet()) {
                    pstmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
                    pstmt.setInt(2, borrowingId);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            showSuccess("Collected $" + String.format("%.2f", totalOwed) + " from " + name + ". Fine cleared.");
            refreshUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to settle fine: " + ex.getMessage());
        }
    }

    // ---------- BookTabPanel (NO Borrow/Return) ----------
    private class BookTabPanel {
        final FilterMode mode;
        final JPanel panel;
        final JTable groupTable;
        final DefaultTableModel groupTableModel;
        final TableRowSorter<DefaultTableModel> groupSorter;
        JTable copyTable;
        DefaultTableModel copyTableModel;
        BookGroup selectedGroup;
        final JButton removeBtn;
        final boolean hasCopyTable;

        BookTabPanel(FilterMode mode, boolean showBorrow, boolean showReturn, boolean showRemove, boolean hasCopyTable) {
            this.mode = mode;
            this.hasCopyTable = hasCopyTable;
            this.panel = new JPanel(new BorderLayout(5, 5));

            // Search bar + Edit button + Delete All button
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.add(new JLabel("Search:"));
            JTextField searchField = new JTextField(20);
            searchPanel.add(searchField);

            JButton editTitleBtn = new JButton("✏️ Edit Title");
            editTitleBtn.addActionListener(e -> editSelectedTitle(this));
            searchPanel.add(editTitleBtn);

            JButton deleteAllBtn = new JButton("🗑️ Delete All Copies");
            deleteAllBtn.addActionListener(e -> deleteAllCopies(this));
            searchPanel.add(deleteAllBtn);

            panel.add(searchPanel, BorderLayout.NORTH);

            // Group table
            String countLabel = (mode == FilterMode.BORROWED) ? "Borrowed / Total" : "Available / Total";
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

            if (hasCopyTable) {
                String[] copyColumns = {"ID", "Status", "Borrow Date", "Return Date"};
                copyTableModel = new DefaultTableModel(copyColumns, 0) {
                    @Override public boolean isCellEditable(int row, int col) { return false; }
                };
                copyTable = new JTable(copyTableModel);
                copyTable.setRowHeight(24);
                copyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                JScrollPane copyScrollPane = new JScrollPane(copyTable);
                copyScrollPane.setBorder(BorderFactory.createTitledBorder("Copies of Selected Title"));

                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                removeBtn = new JButton("🗑️ Remove Selected Copy");
                removeBtn.addActionListener(e -> removeSelectedCopy(this));
                if (showRemove) buttonsPanel.add(removeBtn);

                JPanel copyPanel = new JPanel(new BorderLayout(5, 5));
                copyPanel.add(copyScrollPane, BorderLayout.CENTER);
                copyPanel.add(buttonsPanel, BorderLayout.SOUTH);

                JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, groupScrollPane, copyPanel);
                splitPane.setResizeWeight(0.5);
                panel.add(splitPane, BorderLayout.CENTER);
            } else {
                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                removeBtn = new JButton("🗑️ Remove Selected Title's Copy");
                removeBtn.addActionListener(e -> removeSelectedTitle(this));
                if (showRemove) buttonsPanel.add(removeBtn);

                JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
                tablePanel.add(groupScrollPane, BorderLayout.CENTER);
                tablePanel.add(buttonsPanel, BorderLayout.SOUTH);
                panel.add(tablePanel, BorderLayout.CENTER);
            }
        }

        List<BookGroup> filteredGroups() {
            List<BookGroup> result = new ArrayList<>();
            for (BookGroup g : library.getGroups()) {
                switch (mode) {
                    case ALL: result.add(g); break;
                    case AVAILABLE: if (g.available() > 0) result.add(g); break;
                    case BORROWED: if (g.borrowedCount() > 0) result.add(g); break;
                }
            }
            return result;
        }

        List<Book> filteredCopies(BookGroup group) {
            List<Book> result = new ArrayList<>();
            if (group == null) return result;
            for (Book b : group.getCopies()) {
                switch (mode) {
                    case ALL: result.add(b); break;
                    case AVAILABLE: if (!b.getBorrowed()) result.add(b); break;
                    case BORROWED: if (b.getBorrowed()) result.add(b); break;
                }
            }
            return result;
        }

        void refreshGroupTable() {
            groupTableModel.setRowCount(0);
            for (BookGroup g : filteredGroups()) {
                String countText = (mode == FilterMode.BORROWED)
                        ? (g.borrowedCount() + " / " + g.total())
                        : (g.available() + " / " + g.total());
                groupTableModel.addRow(new Object[]{g.getName(), g.getCategory(), countText, idsToString(g)});
            }
        }

        void refreshCopyTable() {
            if (!hasCopyTable) return;
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
                if (hasCopyTable) copyTableModel.setRowCount(0);
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
            if (hasCopyTable) copyTableModel.setRowCount(0);
        }

        Integer getSelectedCopyId() {
            if (!hasCopyTable) return null;
            int row = copyTable.getSelectedRow();
            if (row < 0) {
                showError("Please select a copy from the table first.");
                return null;
            }
            return (Integer) copyTableModel.getValueAt(row, 0);
        }

        Book getOneBorrowedCopyOfSelectedTitle() {
            int viewRow = groupTable.getSelectedRow();
            if (viewRow < 0) {
                showError("Please select a title from the table first.");
                return null;
            }
            int modelRow = groupTable.convertRowIndexToModel(viewRow);
            String name = (String) groupTableModel.getValueAt(modelRow, 0);
            String category = (String) groupTableModel.getValueAt(modelRow, 1);
            BookGroup group = findGroup(name, category);
            if (group == null) return null;
            for (Book b : group.getCopies()) {
                if (b.getBorrowed()) return b;
            }
            return null;
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

    private void addBook() {
        String name = bookNameField.getText().trim();
        String category = (String) categoryComboBox.getSelectedItem();
        String quantityText = quantityField.getText().trim();

        if (name.isEmpty()) {
            showError("Please enter the name of the book.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                showError("Quantity must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for quantity.");
            return;
        }

        List<Integer> newIds = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Book newBook = new Book(name, category);
            newIds.add(library.addBook(newBook));
        }

        bookNameField.setText("");
        quantityField.setText("1");
        updateStatus();
        selectGroupInAllTabs(name, category);
        showSuccess("Added " + quantity + " copy(ies) of \"" + name + "\".\nIDs: " + newIds);
    }

    private void editSelectedTitle(BookTabPanel tab) {
        int viewRow = tab.groupTable.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select a title from the table first.");
            return;
        }
        int modelRow = tab.groupTable.convertRowIndexToModel(viewRow);
        String oldName = (String) tab.groupTableModel.getValueAt(modelRow, 0);
        String oldCategory = (String) tab.groupTableModel.getValueAt(modelRow, 1);

        String newName = JOptionPane.showInputDialog(this, "Enter new title:", oldName);
        if (newName == null || newName.trim().isEmpty()) {
            showError("Title cannot be empty.");
            return;
        }
        newName = newName.trim();

        JComboBox<String> catCombo = new JComboBox<>(Library.getCategories());
        catCombo.setSelectedItem(oldCategory);
        int result = JOptionPane.showConfirmDialog(this, catCombo, "Select new category:", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String newCategory = (String) catCombo.getSelectedItem();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Update all copies of \"" + oldName + "\" (" + oldCategory + ")\n" +
                        "to \"" + newName + "\" (" + newCategory + ")?",
                "Confirm Update", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (library.updateGroup(oldName, oldCategory, newName, newCategory)) {
            updateStatus();
            selectGroupInAllTabs(newName, newCategory);
            showSuccess("Title updated successfully.");
        } else {
            showError("Failed to update title.");
        }
    }

    private void deleteAllCopies(BookTabPanel tab) {
        int viewRow = tab.groupTable.getSelectedRow();
        if (viewRow < 0) {
            showError("Please select a title from the table first.");
            return;
        }
        int modelRow = tab.groupTable.convertRowIndexToModel(viewRow);
        String name = (String) tab.groupTableModel.getValueAt(modelRow, 0);
        String category = (String) tab.groupTableModel.getValueAt(modelRow, 1);

        BookGroup group = findGroup(name, category);
        if (group == null) {
            showError("Title not found.");
            return;
        }

        int totalCopies = group.total();
        int borrowedCopies = group.borrowedCount();

        String message = "Delete all copies of \"" + name + "\" (" + category + ")?\n\n" +
                "Total copies: " + totalCopies + "\n" +
                "Borrowed copies: " + borrowedCopies + "\n" +
                "Available copies: " + group.available() + "\n\n";

        if (borrowedCopies > 0) {
            message += "⚠️ WARNING: " + borrowedCopies + " copy(ies) are currently BORROWED.\n" +
                    "Deleting this title will also remove borrowed copies from the system.\n\n";
        }

        message += "This action CANNOT be undone!";

        int confirm = JOptionPane.showConfirmDialog(this,
                message,
                "Delete All Copies - Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                loadIcon("images/delete.jpg"));

        if (confirm != JOptionPane.YES_OPTION) return;

        int deleted = library.deleteGroup(name, category);
        if (deleted > 0) {
            updateStatus();
            showSuccess("Deleted " + deleted + " copy(ies) of \"" + name + "\" successfully.");
        } else {
            showError("Failed to delete title. Please check the database.");
        }
    }

    private void removeSelectedCopy(BookTabPanel tab) {
        Integer id = tab.getSelectedCopyId();
        if (id == null) return;
        Book book = library.findBook(id);
        if (book == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete copy #" + id + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, loadIcon("images/delete.jpg"));
        if (confirm == JOptionPane.YES_OPTION) {
            String name = book.getName(), category = book.getCategory();
            if (library.removeBook(id)) {
                updateStatus();
                selectGroupInAllTabs(name, category);
                showSuccess("Book removed successfully.");
            } else {
                showError("Sorry! This book is currently borrowed.");
            }
        }
    }

    private void removeSelectedTitle(BookTabPanel tab) {
        Book book = tab.getOneBorrowedCopyOfSelectedTitle();
        if (book == null) {
            showError("No borrowed copy found for this title.");
            return;
        }
        int id = book.getId();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete copy #" + id + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, loadIcon("images/delete.jpg"));
        if (confirm == JOptionPane.YES_OPTION) {
            String name = book.getName(), category = book.getCategory();
            if (library.removeBook(id)) {
                updateStatus();
                selectGroupInAllTabs(name, category);
                showSuccess("Book removed successfully.");
            } else {
                showError("Sorry! This book is currently borrowed.");
            }
        }
    }

    private void handleMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Operations");
        menuBar.add(menu);

        JMenuItem removeItem = new JMenuItem("Remove Book (by ID)");
        removeItem.addActionListener(e -> removeBookById());
        menu.add(removeItem);

        setJMenuBar(menuBar);
    }

    private void removeBookById() {
        String input = JOptionPane.showInputDialog(this, "Enter Book ID to Remove:");
        if (input == null) return;
        try {
            int id = Integer.parseInt(input);
            Book book = library.findBook(id);
            if (book == null) {
                showError("Book not found.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete book #" + id + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, loadIcon("images/delete.jpg"));
            if (confirm == JOptionPane.YES_OPTION) {
                if (library.removeBook(id)) {
                    updateStatus();
                    showSuccess("Book removed.");
                } else {
                    showError("Book is borrowed.");
                }
            }
        } catch (NumberFormatException e) {
            showError("Invalid ID.");
        }
    }

    private void updateStatus() {
        summaryTextArea.setText(library.getSummary());
        for (BookTabPanel tab : tabPanels) {
            tab.refreshGroupTable();
            tab.refreshCopyTable();
        }
        // Refresh user table if visible
        Component selected = tabbedPane.getSelectedComponent();
        if (selected == userManagementPanel) {
            refreshUsers();
        }
        // Refresh employee table if visible
        if (selected == employeeManagementPanel) {
            refreshEmployees();
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