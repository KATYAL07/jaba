package com.unityeats.model;

/**
 * Enum representing the four user roles in the Unity Eats platform.
 *
 * WHY AN ENUM?
 * Using an enum instead of a plain String column provides:
 * 1. Compile-time type safety - you can't accidentally assign "REASTAURANT"
 * 2. Easy use with Spring Security's role-based access control
 * 3. Stored as a String in the DB (via @Enumerated(EnumType.STRING)) for readability
 *
 * Role Responsibilities:
 * - RESTAURANT: Posts surplus food listings for distribution
 * - NGO:        Browses and accepts available food listings
 * - VOLUNTEER:  Picks up and delivers accepted food; updates status
 * - BENEFICIARY: Views public dashboard of available distributions
 */
public enum Role {
    RESTAURANT,
    NGO,
    VOLUNTEER,
    BENEFICIARY
}
