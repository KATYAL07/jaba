package com.unityeats.dto;

import com.unityeats.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FoodListingResponse DTO - Returned to clients when querying listings.
 *
 * WHY NOT RETURN THE ENTITY DIRECTLY?
 * 1. Lazy Loading: Entity has @ManyToOne(fetch = LAZY) fields. Serializing
 *    the entity directly would cause LazyInitializationException outside a transaction.
 * 2. Security: We don't want to expose all User fields (like hashed passwords).
 * 3. Shape: The frontend needs flat data. The entity has nested User objects.
 *    This DTO flattens it into exactly what the UI needs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodListingResponse {

    private Long id;
    private String foodName;
    private String category;
    private Integer quantity;
    private String unit;
    private String description;
    private String pickupAddress;
    private Integer expiryHours;
    private ListingStatus status;

    // Flattened restaurant info (no need to send the full User object)
    private Long restaurantId;
    private String restaurantName;
    private String restaurantOrg;

    // Flattened NGO info (null if not yet accepted)
    private Long ngoId;
    private String ngoName;

    // Flattened volunteer info (null if not yet assigned)
    private Long volunteerId;
    private String volunteerName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
