package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing and verifying passwords using SHA-256.
 *
 * Note: Plain SHA-256 (without a per-user salt) is used here to keep the
 * implementation simple and consistent with the rest of this project.
 * For a production system, prefer a salted/slow hash (e.g. BCrypt, PBKDF2,
 * or Argon2) since raw SHA-256 is fast and more vulnerable to brute-force
 * / rainbow-table attacks.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";

    private PasswordUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Hashes a plain-text password using SHA-256 and returns it as a
     * lowercase hexadecimal string (64 characters).
     *
     * @param plainPassword the raw password entered by the user
     * @return the SHA-256 hash of the password, hex-encoded
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen - SHA-256 is a standard JDK algorithm
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a plain-text password against a previously stored SHA-256 hash.
     *
     * @param plainPassword  the raw password entered by the user (e.g. at login)
     * @param storedHash     the hash retrieved from the database
     * @return true if the password matches the stored hash, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String computedHash = hashPassword(plainPassword);
        return computedHash.equalsIgnoreCase(storedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}