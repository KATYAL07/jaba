package com.unityeats.dto;

import com.unityeats.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * RegisterRequest DTO - Data Transfer Object for user registration.
 *
 * WHY A SEPARATE DTO?
 * 1. Security: We never expose the raw User entity directly. A DTO acts as
 *    a "contract" for what data the API accepts.
 * 2. Flexibility: We can add fields like 'confirmPassword' that don't map
 *    to any database column.
 * 3. Validation Separation: Entity validations protect the DB layer.
 *    DTO validations protect the API layer. Both must pass.
 *
 * VALIDATION ANNOTATIONS:
 * @NotBlank: Checks for null AND whitespace-only strings (stricter than @NotNull)
 * @Pattern:  Regular expression matching
 * @Email:    Validates email format per RFC 5322
 * @Size:     Validates string length constraints
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[A-Za-z ]{2,100}$",
             message = "Full name must only contain letters and spaces (2-100 characters)")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address (e.g., user@example.com)")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$",
             message = "Phone must be a valid number (10-15 digits, optional '+' prefix for country code)")
    private String phone;

    /**
     * Password policy:
     * Regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
     * - At least 8 characters
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one digit
     * - At least one special character from [@$!%*?&]
     *
     * WHY ENFORCE PASSWORD STRENGTH IN DTO?
     * This is the first layer of defense. If the password is too weak,
     * we reject it immediately with a clear message before any processing.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must contain at least one uppercase, one lowercase, one number, and one special character (@$!%*?&)")
    private String password;

    /**
     * Role must be one of the 4 defined roles.
     * Spring will validate that the string maps to the Role enum.
     */
    @NotNull(message = "Role is required")
    private Role role;

    /**
     * Organization name is optional for VOLUNTEER and BENEFICIARY,
     * but required for RESTAURANT and NGO.
     * Cross-field validation is handled in the Service layer.
     */
    private String organizationName;
}
