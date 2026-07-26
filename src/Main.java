import auth.Login;
import db.DBConnection;
import model.User;
import view.EmployeeDashboard;
import view.UserDashboard;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Shutdown hook to close DB connection when app exits
        Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::closeConnection));

        // Optional quick test
        try {
            Connection conn = DBConnection.getConnection();
            System.out.println("✅ Connected to MySQL successfully!");
            conn.close();
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            // Show login dialog
            Login login = new Login(null);
            login.setVisible(true);
            User loggedUser = login.getLoggedUser();

            if (loggedUser == null) {
                System.exit(0);
                return;
            }

            // Launch appropriate dashboard based on role
            if ("employee".equalsIgnoreCase(loggedUser.getRole())) {
                EmployeeDashboard emp = new EmployeeDashboard();
                emp.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                emp.setVisible(true);
                emp.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                UserDashboard userDash = new UserDashboard(loggedUser);
                userDash.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                userDash.setVisible(true);
                userDash.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }
}