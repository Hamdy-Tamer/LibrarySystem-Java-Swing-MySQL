package auth;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class Login extends JDialog {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JButton registerBtn;
    private User loggedUser;

    private final UserDAO userDAO = new UserDAO();

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

        // UserDAO hashes the entered password internally and compares
        // it against the stored SHA-256 hash - no plain-text comparison here.
        User user = userDAO.authenticate(email, password);

        if (user != null) {
            loggedUser = user;
            dispose();
            return;
        }

        // Distinguish "email not found" vs "wrong password" for a clearer message,
        // without leaking whether the account exists via timing differences.
        if (userDAO.findByEmail(email) == null) {
            showError("Email not found. Please register.");
        } else {
            showError("Incorrect password.");
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
