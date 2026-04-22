package com.unityeats.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtils - Core utility class for JWT operations.
 *
 * WHAT IS JWT?
 * A JSON Web Token consists of 3 base64-encoded parts:
 * 1. Header: {"alg": "HS256", "typ": "JWT"}
 * 2. Payload: {"sub": "user@email.com", "role": "RESTAURANT", "iat": ..., "exp": ...}
 * 3. Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)
 *
 * WHY STATELESS JWT?
 * Traditional session-based auth requires the server to store session data.
 * With JWT, ALL authentication info is in the token itself (signed by the server).
 * The server only needs the secret key to verify the signature - no DB lookup needed
 * on every request. This enables horizontal scaling without shared session storage.
 *
 * SIGNING ALGORITHM: HS256 (HMAC-SHA256)
 * - Symmetric: same key is used for signing AND verification
 * - Good for single-server or trusted-service scenarios
 * - For multi-service architectures, RS256 (asymmetric) is preferred
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * Secret key injected from application.properties.
     * NEVER hardcode secrets in production - use environment variables.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Derive the signing key from the secret string.
     * Keys.hmacShaKeyFor ensures the key is valid for HMAC-SHA256.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for an authenticated user.
     *
     * CLAIMS WE EMBED:
     * - 'sub' (subject): The user's email - standard JWT claim for identity
     * - 'role': Custom claim for role-based authorization on the frontend
     * - 'iat' (issued at): Timestamp of token creation
     * - 'exp' (expiration): Timestamp of token expiry
     *
     * WHY ADD ROLE AS A CLAIM?
     * The frontend can decode the JWT (it's just base64) and read the role
     * to decide which dashboard to show WITHOUT an extra API call.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Extract the role from authorities and add as a custom claim
        // Removes "ROLE_" prefix for cleaner representation in the token
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");

        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername()) // email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the username (email) from a JWT token.
     * The 'sub' claim contains the email.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Check if a token is still valid (not expired AND belongs to this user).
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Generic claim extractor using a function - avoids code duplication
     * for extracting different claim types (String, Date, etc.)
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Parse and validate the JWT, returning all claims.
     *
     * This method will throw exceptions for:
     * - ExpiredJwtException: Token is past its expiration date
     * - MalformedJwtException: Token is not a valid JWT format
     * - SignatureException: Signature doesn't match (token was tampered with)
     * - UnsupportedJwtException: Token uses an unsupported algorithm
     *
     * These are all caught in JwtAuthFilter and result in a 401 response.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Convenience method to validate a token and log the reason for failure.
     * Used in JwtAuthFilter for better debugging.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
