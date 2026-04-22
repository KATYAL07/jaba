package com.unityeats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @NotBlank(message = "Food name cannot be blank")
    @Pattern(regexp = "^[A-Za-z ]{2,100}$",
             message = "Food name must only contain letters and spaces (2-100 characters). No numbers or special characters allowed.")
    @Column(nullable = false)
    private String foodName;

    @NotBlank(message = "Category cannot be blank")
    @Pattern(regexp = "^[A-Za-z ]{2,50}$",
             message = "Category must only contain letters and spaces (2-50 characters)")
    @Column(nullable = false)
    private String category;

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

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Pattern(regexp = "^[^<>]*$",
             message = "Description cannot contain HTML tags")
    @Column(length = 500)
    private String description;

    @NotBlank(message = "Pickup address cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9 ,.-]{5,200}$",
             message = "Pickup address contains invalid characters (5-200 characters)")
    @Column(nullable = false)
    private String pickupAddress;

    @NotNull(message = "Expiry hours cannot be null")
    @Min(value = 1, message = "Food must be valid for at least 1 hour")
    @Max(value = 72, message = "Expiry cannot exceed 72 hours (3 days)")
    @Column(nullable = false)
    private Integer expiryHours;

    // Using EAGER fetch to easily serialize to JSON without LazyInitializationException
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private User restaurant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ngo_id")
    private User acceptedByNgo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "volunteer_id")
    private User assignedVolunteer;

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
