package com.technical.task.task_project.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityServiceTest {

    private SecurityService securityService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(); // Use a real PasswordEncoder (BCryptPasswordEncoder)
        securityService = new SecurityService(passwordEncoder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"password123", "anotherPassword", "123456"})
    public void testEncodePassword(String rawPassword) {
        String encodedPassword = securityService.encodePassword(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @ParameterizedTest
    @CsvSource({
            "password123, true",
            "wrongPassword, false"
    })
    public void testMatchesPassword(String givenPassword, boolean expectedResult) {
        String rawPassword = "password123";
        String encodedPassword = securityService.encodePassword(rawPassword); // Encode a password for testing

        boolean result = securityService.matchesPassword(givenPassword, encodedPassword);

        assertEquals(expectedResult, result);
    }
}
