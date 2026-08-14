package com.community.signal.review.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtTokenProvider(
            @Value("${api.security.jwt.secret}") String secret,
            @Value("${api.security.jwt.expiration-ms}") long expirationMillis) {
        
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("jwt.secret.must.be.at.least.256.bits");
        }
        
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMillis;
        
        log.info("jwt.token.provider.initialized expiration_ms={}", expirationMillis);
    }

    /**
     * Generate JWT token with username as subject and roles claim.
     */
    public String generateToken(String username, List<String> roles) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username.required.for.token.generation");
        }
        
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        
        List<String> safeRoles = (roles != null) ? roles : Collections.emptyList();
        
        String token = Jwts.builder()
            .subject(username)
            .claim("roles", safeRoles)
            .issuedAt(now)
            .expiration(expiry)
            .issuer("review-api")
            .signWith(key, Jwts.SIG.HS512)
            .compact();
        
        log.debug("token.generated username={} roles_count={} expires_in_ms={}", 
            username, safeRoles.size(), expirationMillis);
        
        return token;
    }

    /**
     * Validate token signature and expiration.
     * Returns false for any parsing error without throwing.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("token.validation.failed reason=token_null_or_empty");
            return false;
        }
        
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            
            return true;
            
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("token.validation.failed error_type={} error_message={}", 
                e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Extract username (subject) from valid token.
     * Returns null if token cannot be parsed.
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getSubject();
            
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("token.parse.failed.for.username error={}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract roles claim from token.
     * Returns empty list if claim missing or token invalid.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            Object rolesClaim = claims.get("roles");
            
            if (rolesClaim instanceof List<?>) {
                return (List<String>) rolesClaim;
            }
            
            return Collections.emptyList();
            
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("token.parse.failed.for.roles error={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
    * Returns the configured token expiration time in milliseconds.
    */
    public long getExpirationMillis() {
        return expirationMillis;
    }
}
