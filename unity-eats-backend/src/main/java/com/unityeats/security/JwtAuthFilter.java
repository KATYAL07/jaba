package com.unityeats.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter - The gateway for every secured HTTP request.
 *
 * EXTENDS OncePerRequestFilter:
 * Guarantees this filter runs EXACTLY ONCE per request, even if a request
 * is forwarded or dispatched multiple times internally.
 *
 * FILTER FLOW (for each incoming request):
 * 1. Extract "Authorization: Bearer <token>" header
 * 2. If no token → skip (Spring Security will handle as anonymous)
 * 3. If token present → extract username from JWT
 * 4. Load UserDetails from DB using username
 * 5. Validate token (signature + expiry)
 * 6. If valid → set Authentication in SecurityContext
 * 7. Continue filter chain (request reaches Controller)
 *
 * WHAT IS SecurityContext?
 * A thread-local storage that holds the currently authenticated user for this request.
 * Once set here, @PreAuthorize annotations and SecurityContextHolder.getContext()
 * throughout the request can access the authenticated user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Step 1: Extract JWT from the Authorization header
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtUtils.validateToken(jwt)) {
                // Step 2: Extract username (email) from the valid token
                String username = jwtUtils.extractUsername(jwt);

                // Step 3: Load the full UserDetails from DB
                // WHY FROM DB? The token only has email & role. We need the full
                // UserDetails (authorities, enabled status, etc.) for Spring Security.
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 4: Double-check token is valid for this specific user
                if (jwtUtils.isTokenValid(jwt, userDetails)) {

                    // Step 5: Create an Authentication object
                    // UsernamePasswordAuthenticationToken is Spring's standard auth token
                    // Passing 'null' as credentials (password) because we don't need it
                    // after successful JWT validation
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Attach request details (IP, session ID) to the auth token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 6: Store authentication in SecurityContext
                    // This is what makes @PreAuthorize and hasRole() work
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Don't throw - let the filter chain continue.
            // Spring Security will handle the unauthenticated request appropriately.
        }

        // Step 7: Always continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     *
     * Standard format: "Authorization: Bearer eyJhbGci..."
     * We strip the "Bearer " prefix to get the raw token.
     *
     * WHY NOT COOKIES?
     * We use the Authorization header (not cookies) because:
     * 1. Works for mobile apps and SPAs equally
     * 2. Not vulnerable to CSRF attacks (cookies are automatically sent by browsers)
     * 3. Standard REST API practice
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " (7 chars)
        }
        return null;
    }
}
