package com.unityeats.security;

import com.unityeats.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig - The central Spring Security configuration.
 *
 * KEY CONCEPTS:
 *
 * 1. STATELESS SESSIONS:
 *    SessionCreationPolicy.STATELESS tells Spring Security to NEVER create
 *    an HTTP session. Every request must authenticate via JWT.
 *    This is crucial for RESTful APIs.
 *
 * 2. CSRF DISABLED:
 *    CSRF (Cross-Site Request Forgery) protection is for session-based auth.
 *    Since we're stateless (JWT), CSRF tokens are unnecessary and we disable them.
 *
 * 3. CORS CONFIGURATION:
 *    Since the frontend runs on a different port (5500) than the backend (8080),
 *    we must configure CORS to allow cross-origin requests.
 *
 * 4. FILTER ORDER:
 *    Our JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter.
 *    This ensures JWT auth happens first, before Spring's default form-based auth.
 *
 * 5. METHOD SECURITY (@EnableMethodSecurity):
 *    Enables @PreAuthorize annotations on individual controller methods,
 *    allowing fine-grained role-based access control beyond URL patterns.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize at method level
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    public SecurityConfig(@org.springframework.context.annotation.Lazy JwtAuthFilter jwtAuthFilter, UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
    }

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * UserDetailsService - Loads user from DB by email.
     *
     * WHY DEFINE THIS AS A @Bean?
     * Spring Security's DaoAuthenticationProvider needs a UserDetailsService
     * to know HOW to look up users. We define it here so it can be injected
     * into the AuthenticationProvider.
     *
     * LAMBDA PATTERN: We use a lambda instead of a separate class because
     * the implementation is trivial - just find by email or throw.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));
    }

    /**
     * PasswordEncoder - BCrypt hashing for passwords.
     *
     * WHY BCrypt?
     * - Intentionally slow (CPU-intensive) - makes brute force attacks impractical
     * - Automatically generates and stores a random salt per password
     * - Work factor can be increased over time as hardware gets faster
     * - Industry standard for password storage
     *
     * NEVER store plain text passwords. NEVER use MD5 or SHA-1.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationProvider - Ties together UserDetailsService + PasswordEncoder.
     *
     * DaoAuthenticationProvider:
     * 1. Loads user via userDetailsService.loadUserByUsername(email)
     * 2. Checks submitted password against stored BCrypt hash
     * 3. Returns Authentication object if valid
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager - The entry point for programmatic authentication.
     * Used in AuthService.login() to trigger the authentication flow.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SecurityFilterChain - Defines which endpoints require authentication and how.
     *
     * URL PATTERN BREAKDOWN:
     * - /api/auth/** : Public (no auth needed for login/register)
     * - /api/public/** : Public (beneficiary dashboard, no auth needed)
     * - /h2-console/** : Public (dev tool, would be disabled in production)
     * - Everything else : Authenticated (valid JWT required)
     *
     * NOTE: More granular role checks are done with @PreAuthorize on controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS with our custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no token required
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                // All other endpoints require a valid JWT
                .anyRequest().authenticated()
            )

            // Allow H2 console to render in iframes (it uses frames)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            // STATELESS: No HTTP session will be created or used
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom authentication provider
            .authenticationProvider(authenticationProvider())

            // Add JWT filter BEFORE Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration - Allows the frontend to call our API.
     *
     * Without this, browsers block cross-origin requests (same-origin policy).
     * allowedOrigins must match EXACTLY where the frontend is served from.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins from application.properties
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow Authorization header (needed for Bearer token) and Content-Type
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

        // Allow the browser to read the Authorization header in responses
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
