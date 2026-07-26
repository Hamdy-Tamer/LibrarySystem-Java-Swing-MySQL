package auth;

import db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Register extends JDialog {

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerBtn;
    private JButton loginBtn;

    public Register(JFrame parent) {
        super(parent, "Register - New User", true);
        setSize(480, 420);
        setLocationRelativeTo(parent);

        ImageIcon icon = loadIcon("images/authentication.png");
        if (icon != null) setIconImage(icon.getImage());

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, gbc);
        gbc.gridwidth = 1;

        // First Name (row 1, col 0-1)
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        firstNameField = new JTextField(12);
        add(firstNameField, gbc);

        // Last Name (row 1, col 2-3)
        gbc.gridx = 2;
        add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 3;
        lastNameField = new JTextField(12);
        add(lastNameField, gbc);

        // Email (row 2)
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        emailField = new JTextField(25);
        add(emailField, gbc);
        gbc.gridwidth = 1;

        // Phone (row 3)
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Phone (11 digits):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        phoneField = new JTextField(25);
        add(phoneField, gbc);
        gbc.gridwidth = 1;

        // Password (row 4)
        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        passwordField = new JPasswordField(25);
        add(passwordField, gbc);
        gbc.gridwidth = 1;

        // Confirm Password (row 5)
        gbc.gridx = 0; gbc.gridy = 5;
        add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        confirmPasswordField = new JPasswordField(25);
        add(confirmPasswordField, gbc);
        gbc.gridwidth = 1;

        // Buttons Panel (row 6)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        registerBtn = new JButton("Register");
        loginBtn = new JButton("Login");

        // Set button colors for visibility
        registerBtn.setBackground(new Color(50, 150, 50));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);

        loginBtn.setBackground(new Color(70, 130, 180));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);

        registerBtn.addActionListener(e -> registerUser());
        loginBtn.addActionListener(e -> switchToLogin());

        buttonPanel.add(registerBtn);
        buttonPanel.add(loginBtn);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 4;
        add(buttonPanel, gbc);

        // "Already have an account?" label (row 7)
        gbc.gridy = 7;
        JLabel switchLabel = new JLabel("Already have an account? Click 'Login' above.");
        switchLabel.setForeground(Color.GRAY);
        switchLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        switchLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(switchLabel, gbc);

        getRootPane().setDefaultButton(registerBtn);
    }

    private void registerUser() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        // --- Validations ---
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showError("First and Last name are required.");
            return;
        }
        if (!firstName.matches("[a-zA-Z]{3,15}")) {
            showError("First name must be 3-15 letters only (no spaces).");
            return;
        }
        if (!lastName.matches("[a-zA-Z]{3,15}")) {
            showError("Last name must be 3-15 letters only (no spaces).");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (!phone.matches("\\d{11}")) {
            showError("Phone number must be exactly 11 digits.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // --- Insert into database ---
        try (Connection conn = DBConnection.getConnection()) {
            // Check if email already exists
            String checkEmailSql = "SELECT email FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql)) {
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    showError("This email is already registered. Please log in.");
                    return;
                }
            }

            // Check if phone number already exists
            String checkPhoneSql = "SELECT phone_number FROM users WHERE phone_number = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkPhoneSql)) {
                checkStmt.setString(1, phone);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    showError("This phone number is already registered. Please use a different number.");
                    return;
                }
            }

            // Insert user (role = 'user')
            String insertUser = "INSERT INTO users (first_name, last_name, email, password, phone_number, role) VALUES (?, ?, ?, ?, ?, 'user')";
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
                    showError("Registration failed. Please try again.");
                    return;
                }
            }

            // Insert into members table
            String memberCode = "M" + System.currentTimeMillis();
            String insertMember = "INSERT INTO members (user_id, member_code) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertMember)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, memberCode);
                pstmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(this,
                    "Registration successful!\nYou can now log in.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE,
                    loadIcon("images/success.jpg"));
            dispose();
            // Automatically switch to login after successful registration
            JFrame parentFrame = getParentFrame();
            Login login = new Login(parentFrame);
            login.setVisible(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Database error: " + ex.getMessage());
        }
    }

    private void switchToLogin() {
        dispose();
        JFrame parentFrame = getParentFrame();
        Login login = new Login(parentFrame);
        login.setVisible(true);
    }

    private JFrame getParentFrame() {
        Window owner = getOwner();
        if (owner instanceof JFrame) {
            return (JFrame) owner;
        } else {
            // Create a temporary parent frame
            JFrame tempFrame = new JFrame();
            tempFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return tempFrame;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE,
                loadIcon("images/error.jpg"));
    }

    private ImageIcon loadIcon(String path) {
        File file = new File(path);
        if (file.exists()) return new ImageIcon(path);
        return null;
    }
}