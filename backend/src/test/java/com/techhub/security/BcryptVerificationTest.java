package com.techhub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BcryptVerificationTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    // Hashes from data.sql (all 3 users use the same hash)
    private static final String ADMIN_HASH = "$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2";
    private static final String MODERATOR_HASH = "$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2";
    private static final String USER_HASH = "$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2";

    @Test
    public void testAdminPasswordMatches() {
        assertTrue(encoder.matches("123456", ADMIN_HASH),
            "Admin password '123456' should match the stored BCrypt hash");
    }

    @Test
    public void testModeratorPasswordMatches() {
        assertTrue(encoder.matches("123456", MODERATOR_HASH),
            "Moderator password '123456' should match the stored BCrypt hash");
    }

    @Test
    public void testUserPasswordMatches() {
        assertTrue(encoder.matches("123456", USER_HASH),
            "User password '123456' should match the stored BCrypt hash");
    }

    @Test
    public void testWrongPasswordDoesNotMatch() {
        // Sanity check that wrong passwords are rejected
        assertTrue(!encoder.matches("wrongpassword", ADMIN_HASH),
            "Wrong password should NOT match the stored BCrypt hash");
    }
}
