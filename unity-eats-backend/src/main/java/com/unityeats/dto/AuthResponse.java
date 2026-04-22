package com.unityeats.dto;

import com.unityeats.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse DTO - Returned to the client after successful login/registration.
 *
 * WHY RETURN ALL THESE FIELDS?
 * The frontend (Vanilla JS) needs to:
 * 1. Store the token in localStorage to attach to future API requests
 * 2. Know the user's role to render the correct dashboard
 * 3. Show the user's name in the UI header
 *
 * Returning all this in one response avoids a follow-up /me API call
 * after login, reducing initial load time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT Bearer token - attach as 'Authorization: Bearer <token>' header */
    private String token;

    private String tokenType = "Bearer";

    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private String organizationName;
    private String message;
}
