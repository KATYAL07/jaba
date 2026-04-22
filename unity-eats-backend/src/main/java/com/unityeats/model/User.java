package com.unityeats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User Entity - The core authentication entity for the platform.
 *
 * IMPLEMENTS UserDetails:
 * By implementing Spring Security's UserDetails, this entity can be used
 * directly by the authentication framework without a separate adapter class.
 * Spring Security will call getUsername(), getPassword(), getAuthorities()
 * automatically during the authentication process.
 *
 * VALIDATION STRATEGY:
 * We validate at TWO levels:
 * 1. Entity Level (@NotBlank, @Pattern, etc.) - last defense, always runs before DB write
 * 2. DTO Level (RegisterRequest) - validates user input before even reaching the service
 *
 * This "defense in depth" ensures bad data never reaches the database.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Full name validation:
     * - Must not be blank (null or whitespace)
     * - Only letters and spaces (no numbers, no special characters)
     * - The regex ^[A-Za-z ]+$ enforces this rule strictly
     *
     * WHY REGEX ON ENTITY? Even if the DTO catches bad input,
     * this prevents any programmatic bypass of the DTO layer.
     */
    @NotBlank(message = "Full name cannot be blank")
    @Pattern(regexp = "^[A-Za-z ]{2,100}$",
             message = "Full name must only contain letters and spaces (2-100 characters)")
    @Column(nullable = false)
    private String fullName;

    /**
     * Email validation using Jakarta's built-in @Email constraint.
     * @Email validates the RFC 5322 format (user@domain.tld)
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid format (e.g., user@example.com)")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Phone validation:
     * Regex: ^\+?[1-9]\d{9,14}$
     * - Optional leading '+' for international format
     * - First digit 1-9 (no country codes starting with 0)
     * - Total 10-15 digits (standard international phone length)
     */
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$",
             message = "Phone must be a valid number (10-15 digits, optional '+' prefix)")
    @Column(nullable = false)
    private String phone;

    /**
     * Password is stored as a BCrypt hash.
     * We don't validate the raw password here - that happens in RegisterRequest DTO.
     * The hashed value stored here will always be a valid Bcrypt string.
     */
    @NotBlank(message = "Password cannot be blank")
    @Column(nullable = false)
    private String password;

    /**
     * Role stored as String in DB for readability (not as ordinal integer).
     * @Enumerated(EnumType.STRING) ensures "RESTAURANT" is stored, not "0".
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Organization name - required for RESTAURANT and NGO roles.
     * Optional for VOLUNTEER and BENEFICIARY.
     */
    @Column
    private String organizationName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================================
    // Spring Security UserDetails Implementation
    // ============================================================

    /**
     * Converts our single Role enum to a Spring Security authority.
     * Spring Security expects the "ROLE_" prefix for role-based checks
     * like @PreAuthorize("hasRole('RESTAURANT')")
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security uses getUsername() to identify the user.
     * We use email as the unique identifier (username).
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
