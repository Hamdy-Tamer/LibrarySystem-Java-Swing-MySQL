package auth;

import db.DBConnection;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login extends JDialog {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JButton registerBtn;
    private User loggedUser;

    public Login(JFrame parent) {
        super(parent, "Login", true);
        setSize(400, 250);
        setLocationRelativeTo(parent);

        ImageIcon icon = loadIcon("images/authentication.png");
        if (icon != null) setIconImage(icon.getImage());

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Library Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, gbc);
        gbc.gridwidth = 1;

        // Email
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        add(passwordField, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        loginBtn = new JButton("Login");
        registerBtn = new JButton("Register");

        // Set button colors for visibility
        loginBtn.setBackground(new Color(50, 150, 50));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);

        registerBtn.setBackground(new Color(70, 130, 180));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);

        loginBtn.addActionListener(e -> login());
        registerBtn.addActionListener(e -> switchToRegister());

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        // "Don't have an account?" label
        gbc.gridy = 4;
        JLabel switchLabel = new JLabel("Don't have an account? Click 'Register' above.");
        switchLabel.setForeground(Color.GRAY);
        switchLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        switchLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(switchLabel, gbc);

        getRootPane().setDefaultButton(loginBtn);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Do nothing – user may cancel
            }
        });
    }

    private void login() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT user_id, first_name, last_name, email, phone_number, role, password FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (storedPassword.equals(password)) {
                        loggedUser = new User();
                        loggedUser.setUserId(rs.getInt("user_id"));
                        loggedUser.setFirstName(rs.getString("first_name"));
                        loggedUser.setLastName(rs.getString("last_name"));
                        loggedUser.setEmail(rs.getString("email"));
                        loggedUser.setPhoneNumber(rs.getString("phone_number"));
                        loggedUser.setRole(rs.getString("role"));
                        dispose();
                        return;
                    } else {
                        showError("Incorrect password.");
                    }
                } else {
                    showError("Email not found. Please register.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Database error: " + ex.getMessage());
        }
    }

    private void switchToRegister() {
        dispose();
        // Get the parent frame safely
        JFrame parentFrame = getParentFrame();
        Register register = new Register(parentFrame);
        register.setVisible(true);
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

    public User getLoggedUser() {
        return loggedUser;
    }
}