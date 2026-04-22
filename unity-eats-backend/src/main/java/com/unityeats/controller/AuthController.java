package com.unityeats.controller;

import com.unityeats.dto.AuthResponse;
import com.unityeats.dto.LoginRequest;
import com.unityeats.dto.RegisterRequest;
import com.unityeats.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Handles user registration and login.
 *
 * CONTROLLER RESPONSIBILITIES:
 * 1. Receive HTTP requests
 * 2. Validate input DTOs with @Valid (triggers jakarta validation)
 * 3. Delegate to Service for business logic
 * 4. Return appropriate HTTP responses
 *
 * WHAT CONTROLLERS MUST NOT DO:
 * - Business logic (that's Service's job)
 * - Direct database access (that's Repository's job)
 * - Password hashing, JWT generation (that's Service + JwtUtils)
 *
 * These endpoints are PUBLIC (no auth required) - configured in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * @Valid triggers validation of RegisterRequest fields.
     * If any @NotBlank, @Pattern, @Email constraint fails,
     * Spring throws MethodArgumentNotValidException BEFORE this method body runs.
     * GlobalExceptionHandler catches it and returns a 400 with field errors.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        // 201 Created is semantically correct for resource creation (new user)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Returns 200 OK with JWT token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
