package com.unityeats.model;

/**
 * Enum representing the lifecycle states of a food donation listing.
 *
 * STATE MACHINE DIAGRAM:
 *
 *   AVAILABLE ──(NGO accepts)──► ACCEPTED ──(Volunteer assigned)──► ASSIGNED
 *                                                                        │
 *                                                        (Volunteer picks up)
 *                                                                        ▼
 *                                                                   PICKED_UP
 *                                                                        │
 *                                                        (Volunteer delivers)
 *                                                                        ▼
 *                                                                   DELIVERED
 *
 * WHY TRACK STATUS ON THE ENTITY?
 * Storing status directly on FoodListing means we can query by status easily
 * using Spring Data JPA derived queries (findByStatus), avoiding complex JOINs.
 */
public enum ListingStatus {
    AVAILABLE,   // Just posted by Restaurant - visible to all NGOs
    ACCEPTED,    // Claimed by an NGO - awaiting volunteer assignment
    ASSIGNED,    // A Volunteer has taken on the delivery
    PICKED_UP,   // Volunteer confirmed pickup from restaurant
    DELIVERED    // Food successfully delivered to beneficiaries
}
