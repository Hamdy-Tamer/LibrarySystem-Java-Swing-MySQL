package dao;

import db.DBConnection;
import model.User;
import util.PasswordUtil;

import java.sql.*;

/**
 * Data access object for the `users` table.
 * Handles registration, authentication and lookups, and ensures
 * passwords are always hashed (SHA-256) before touching the database.
 */
public class UserDAO {

    /**
     * Looks up a user by email. The returned User's password field
     * contains the stored HASH, not the plain-text password.
     */
    public User findByEmail(String email) {
        String sql = "SELECT user_id, first_name, last_name, email, phone_number, role, password "
                + "FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean phoneExists(String phone) {
        String sql = "SELECT user_id FROM users WHERE phone_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registers a new user with role='user', hashing the password with
     * SHA-256 before storing it, and creates the matching `members` row.
     * The plain-text password on the passed-in User object is expected;
     * this method replaces it with the hash on success.
     *
     * @return the generated user_id, or -1 on failure
     */
    public int registerUser(User user) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String hashedPassword = PasswordUtil.hashPassword(user.getPassword());

            String insertUser = "INSERT INTO users (first_name, last_name, email, password, phone_number, role) "
                    + "VALUES (?, ?, ?, ?, ?, 'user')";
            int userId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, user.getFirstName());
                pstmt.setString(2, user.getLastName());
                pstmt.setString(3, user.getEmail());
                pstmt.setString(4, hashedPassword);
                pstmt.setString(5, user.getPhoneNumber());
                pstmt.executeUpdate();

                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        userId = keys.getInt(1);
                    } else {
                        conn.rollback();
                        return -1;
                    }
                }
            }

            String memberCode = "M" + System.currentTimeMillis();
            String insertMember = "INSERT INTO members (user_id, member_code) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertMember)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, memberCode);
                pstmt.executeUpdate();
            }

            conn.commit();
            user.setUserId(userId);
            user.setPassword(hashedPassword);
            user.setRole("user");
            return userId;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Authenticates a user by email + plain-text password.
     *
     * @return the User (with hashed password field) if credentials are valid, otherwise null
     */
    public User authenticate(String email, String plainPassword) {
        User user = findByEmail(email);
        if (user == null) {
            return null;
        }
        if (PasswordUtil.verifyPassword(plainPassword, user.getPassword())) {
            return user;
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPhoneNumber(rs.getString("phone_number"));
        u.setRole(rs.getString("role"));
        u.setPassword(rs.getString("password")); // this is the HASH, not plain text
        return u;
    }
}