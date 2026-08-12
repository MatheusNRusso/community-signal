package com.community.signal.review.security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class JwtTokenProviderTest {
    private JwtTokenProvider provider;
    private static final String SECRET = "community-signal-review-secret-key-for-testing-must-be-64-chars!";
    private static final long EXPIRATION = 86400000L;
    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION);
    }
    @Test
    void generateToken_validInput_returnsNonNullToken() {
        String token = provider.generateToken("reviewer-1", List.of("ROLE_REVIEWER"));
        assertNotNull(token, "token should not be null");
        assertFalse(token.isBlank(), "token should not be blank");
        long dotCount = token.chars().filter(ch -> ch == '.').count();
        assertEquals(3, token.split("\\.").length, "JWT should have 3 parts separated by 2 dots");
        assertEquals(2, dotCount, "JWT should contain exactly 2 dots");
    }
    @Test
    void validateToken_validToken_returnsTrue() {
        String token = provider.generateToken("reviewer-1", List.of("ROLE_REVIEWER"));
        assertTrue(provider.validateToken(token), "valid token should pass validation");
    }
    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(provider.validateToken(null), "null token should fail validation");
    }
    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(provider.validateToken("not.a.valid.token"), "malformed token should fail validation without throwing exception");
    }
    @Test
    void getUsernameFromToken_validToken_returnsUsername() {
        String token = provider.generateToken("reviewer-1", List.of("ROLE_REVIEWER"));
        assertEquals("reviewer-1", provider.getUsernameFromToken(token), "extracted username should match input");
    }
    @Test
    void getRolesFromToken_validToken_returnsRoles() {
        String token = provider.generateToken("reviewer-1", List.of("ROLE_REVIEWER"));
        List<String> roles = provider.getRolesFromToken(token);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains("ROLE_REVIEWER"));
    }
}
