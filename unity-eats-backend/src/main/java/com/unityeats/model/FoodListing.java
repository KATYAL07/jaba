package com.unityeats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FoodListing Entity - Represents a surplus food donation posted by a Restaurant.
 *
 * LIFECYCLE:
 * This entity is the heart of the Unity Eats platform. Its 'status' field
 * drives the entire donation workflow (AVAILABLE -> ACCEPTED -> ASSIGNED
 * -> PICKED_UP -> DELIVERED).
 *
 * RELATIONSHIPS:
 * We use @ManyToOne to link listings to their actors (restaurant, NGO, volunteer).
 * This is intentionally NOT bidirectional (@OneToMany on User side) to keep
 * the User entity clean and avoid accidental data loading (N+1 queries).
 *
 * VALIDATION MASTERCLASS:
 * Food names must be ONLY alphabetic - restaurants can't enter "Chicken 2.0" or
 * "Food#123". This prevents XSS injection attempts through the food name field,
 * as malicious scripts often contain special characters.
 */
@Entity
@Table(name = "food_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Food name validation - STRICT ALPHABETIC ONLY.
     *
     * Regex: ^[A-Za-z ]{2,100}$
     * - ONLY letters and spaces - NO numbers, NO special chars
     * - Why? Food names like "Rice" or "Pav Bhaji" are valid.
     *   "Rice123" or "Food@#$%" suggest data entry errors or injection attempts.
     * - Minimum 2 chars to prevent single-character nonsense entries.
     */
    @NotBlank(message = "Food name cannot be blank")
    @Pattern(regexp = "^[A-Za-z ]{2,100}$",
             message = "Food name must only contain letters and spaces (2-100 characters). No numbers or special characters allowed.")
    @Column(nullable = false)
    private String foodName;

    /**
     * Category must also be strictly alphabetic.
     * Valid: "Cooked Meals", "Bakery", "Dairy"
     * Invalid: "Food_Type2", "Cat#1"
     */
    @NotBlank(message = "Category cannot be blank")
    @Pattern(regexp = "^[A-Za-z ]{2,50}$",
             message = "Category must only contain letters and spaces (2-50 characters)")
    @Column(nullable = false)
    private String category;

    /**
     * Quantity validation - MUST BE A POSITIVE INTEGER.
     *
     * @Min(1): Ensures at least 1 serving/unit. A quantity of 0 or negative
     * makes no logical sense for food redistribution.
     * @Max(10000): Reasonable upper bound to catch accidental entries like 999999
     */
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity cannot exceed 10,000 units")
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank(message = "Unit cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{2,20}$",
             message = "Unit must only contain letters (e.g., servings, kg, liters)")
    @Column(nullable = false)
    private String unit;

    /**
     * Description is optional but if provided, must not contain script tags.
     * The pattern disallows '<' and '>' to prevent XSS in descriptions.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Pattern(regexp = "^[^<>]*$",
             message = "Description cannot contain HTML tags")
    @Column(length = 500)
    private String description;

    /**
     * Address validation: Letters, numbers, spaces, commas, hyphens, and periods.
     * Excludes most special characters that don't appear in legitimate addresses.
     */
    @NotBlank(message = "Pickup address cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9 ,.-]{5,200}$",
             message = "Pickup address contains invalid characters (5-200 characters)")
    @Column(nullable = false)
    private String pickupAddress;

    /**
     * Expiry hours: How many hours until the food expires.
     * @Min(1): Must be at least 1 hour - 0 or negative doesn't make sense
     * @Max(72): Food should be redistributed within 3 days max
     */
    @NotNull(message = "Expiry hours cannot be null")
    @Min(value = 1, message = "Food must be valid for at least 1 hour")
    @Max(value = 72, message = "Expiry cannot exceed 72 hours (3 days)")
    @Column(nullable = false)
    private Integer expiryHours;

    // ============================================================
    // Relationship Fields - Set by the Service Layer
    // ============================================================

    /**
     * The restaurant that posted this listing.
     * FetchType.LAZY is best practice - don't load the full User object
     * every time you query FoodListing.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private User restaurant;

    /**
     * The NGO that accepted this listing. Null until accepted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_id")
    private User acceptedByNgo;

    /**
     * The volunteer assigned to deliver this listing. Null until assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id")
    private User assignedVolunteer;

    /**
     * Current status in the donation lifecycle.
     * Defaults to AVAILABLE when first posted.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
