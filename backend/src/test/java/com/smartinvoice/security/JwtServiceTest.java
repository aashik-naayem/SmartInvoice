package com.smartinvoice.security;

import com.smartinvoice.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtService has no Spring dependencies of its own beyond @Value-injected config, so these run
 * as plain unit tests - the @Value fields are set manually via ReflectionTestUtils since there's
 * no application context here to inject them.
 */
class JwtServiceTest {

    // Test-only key, unrelated to anything used in the real app - never reuse a key like this.
    private static final String TEST_SECRET =
            "dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLWp3dHNlcnZpY2V0ZXN0LWRvLW5vdC11c2UtaW4tcHJvZA==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600_000L); // 1 hour
    }

    @Test
    void generateToken_roundTripsTheSubjectEmail() {
        String token = jwtService.generateToken("ashik@example.com", Role.USER);

        assertThat(jwtService.extractEmail(token)).isEqualTo("ashik@example.com");
    }

    @Test
    void isTokenValid_trueForMatchingUserAndUnexpiredToken() {
        String token = jwtService.generateToken("ashik@example.com", Role.USER);
        User userDetails = new User("ashik@example.com", "irrelevant", java.util.List.of());

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_falseWhenTokenBelongsToADifferentUser() {
        String token = jwtService.generateToken("ashik@example.com", Role.USER);
        User someoneElse = new User("someone-else@example.com", "irrelevant", java.util.List.of());

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }

    @Test
    void isTokenValid_throwsExpiredJwtExceptionForAnExpiredToken() {
        // JwtService itself doesn't swallow this - jjwt throws while parsing an expired token's
        // claims, before isTokenValid ever gets to compare expiry. JwtAuthenticationFilter is the
        // layer that catches this (see its broad catch block) and treats it as "not authenticated"
        // rather than letting it bubble up as a 500. Documenting that boundary here.
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // already expired
        String token = jwtService.generateToken("ashik@example.com", Role.USER);
        User userDetails = new User("ashik@example.com", "irrelevant", java.util.List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
