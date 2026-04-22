package com.unityeats.service;

import com.unityeats.dto.AuthResponse;
import com.unityeats.dto.LoginRequest;
import com.unityeats.dto.RegisterRequest;
import com.unityeats.exception.BusinessException;
import com.unityeats.model.Role;
import com.unityeats.model.User;
import com.unityeats.repository.UserRepository;
import com.unityeats.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService - Business logic for authentication operations.
 *
 * SERVICE LAYER RESPONSIBILITIES:
 * - Enforce business rules that DTOs can't express (e.g., cross-field validation)
 * - Coordinate between repositories (data access) and controllers (HTTP)
 * - Handle transactions (@Transactional ensures DB rollback on failure)
 * - NOT responsible for HTTP concerns (status codes, headers) - that's the Controller's job
 *
 * TRANSACTION DESIGN:
 * @Transactional on the class means ALL methods are transactional by default.
 * If any exception occurs mid-method, all DB changes in that method are rolled back.
 * For read-only operations, use @Transactional(readOnly = true) for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user on the platform.
     *
     * FLOW:
     * 1. Check email uniqueness (business rule, can't be expressed in DTO)
     * 2. Enforce cross-field validation (RESTAURANT/NGO must have org name)
     * 3. Hash the password (NEVER store plain text)
     * 4. Save user to DB
     * 5. Generate JWT and return AuthResponse
     *
     * WHY RETURN JWT ON REGISTRATION?
     * Better UX - user is automatically logged in after registering.
     * No need for a separate login step.
     */
    public AuthResponse register(RegisterRequest request) {
        // BUSINESS RULE 1: Email must be unique
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict(
                "An account with email '" + request.getEmail() + "' already exists. Please login instead."
            );
        }

        // BUSINESS RULE 2: RESTAURANT and NGO must provide an organization name
        if ((request.getRole() == Role.RESTAURANT || request.getRole() == Role.NGO)
                && (request.getOrganizationName() == null || request.getOrganizationName().isBlank())) {
            throw BusinessException.badRequest(
                "Organization name is required for " + request.getRole() + " accounts."
            );
        }

        // Build the User entity using Lombok's @Builder pattern
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                // CRITICAL: Hash password with BCrypt - NEVER store plain text
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .organizationName(request.getOrganizationName() != null
                        ? request.getOrganizationName().trim() : null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role {}", savedUser.getEmail(), savedUser.getRole());

        // Generate JWT for the newly registered user
        String token = jwtUtils.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .organizationName(savedUser.getOrganizationName())
                .message("Registration successful! Welcome to Unity Eats.")
                .build();
    }

    /**
     * Authenticate an existing user and return a JWT.
     *
     * FLOW:
     * 1. Use Spring's AuthenticationManager to verify credentials
     *    - It calls our DaoAuthenticationProvider
     *    - Which loads user from DB and compares BCrypt hashes
     * 2. If valid, extract the authenticated User
     * 3. Generate a fresh JWT
     * 4. Return AuthResponse
     *
     * WHY USE AuthenticationManager INSTEAD OF MANUAL COMPARISON?
     * AuthenticationManager handles all edge cases (locked accounts, expired credentials,
     * disabled accounts) automatically via the UserDetails contract.
     * Manual password comparison would bypass these checks.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // This throws BadCredentialsException if invalid - caught by GlobalExceptionHandler
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // Cast the authenticated principal to our User entity
        User user = (User) authentication.getPrincipal();

        log.info("User logged in: {} ({})", user.getEmail(), user.getRole());

        String token = jwtUtils.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .organizationName(user.getOrganizationName())
                .message("Login successful! Welcome back, " + user.getFullName() + ".")
                .build();
    }
}
