package com.unityeats.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * FoodListingRequest DTO - Used when a Restaurant posts a new food listing.
 *
 * This DTO demonstrates "strict validation" as required by the spec:
 * - Food names: ONLY letters and spaces
 * - Quantities: Positive integers only
 * - All fields are validated BEFORE reaching the Service layer
 *
 * DESIGN DECISION: The restaurant_id is NOT included here.
 * Instead, we extract it from the JWT token in the Controller/Service.
 * This prevents a logged-in restaurant from posting food on behalf of
 * another restaurant by manipulating the request body.
 */
@Data
public class FoodListingRequest {

    @NotBlank(message = "Food name is required")
    @Pattern(regexp = "^[A-Za-z ]{2,100}$",
             message = "Food name must only contain letters and spaces. No numbers or special characters.")
    private String foodName;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "^[A-Za-z ]{2,50}$",
             message = "Category must only contain letters and spaces")
    private String category;

    /**
     * Using Integer (object) not int (primitive) so @NotNull can detect null.
     * A primitive int defaults to 0 and cannot be null, making @NotNull useless.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity cannot exceed 10,000")
    private Integer quantity;

    @NotBlank(message = "Unit is required (e.g., servings, kg, liters)")
    @Pattern(regexp = "^[A-Za-z]{2,20}$",
             message = "Unit must only contain letters")
    private String unit;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Pattern(regexp = "^[^<>]*$", message = "Description cannot contain HTML tags")
    private String description;

    @NotBlank(message = "Pickup address is required")
    @Pattern(regexp = "^[A-Za-z0-9 ,.-]{5,200}$",
             message = "Pickup address contains invalid characters")
    private String pickupAddress;

    @NotNull(message = "Expiry hours is required")
    @Min(value = 1, message = "Food must be valid for at least 1 hour")
    @Max(value = 72, message = "Expiry cannot exceed 72 hours")
    private Integer expiryHours;
}
