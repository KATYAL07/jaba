package com.unityeats.controller;

import com.unityeats.dto.FoodListingRequest;
import com.unityeats.dto.FoodListingResponse;
import com.unityeats.model.ListingStatus;
import com.unityeats.service.FoodListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FoodListingController - REST endpoints for the food donation lifecycle.
 *
 * @PreAuthorize EXPLAINED:
 * This annotation uses Spring Security's SpEL (Spring Expression Language) to
 * check the current user's role BEFORE the method executes.
 *
 * - hasRole('RESTAURANT'): User must have "ROLE_RESTAURANT" authority
 * - hasAnyRole('NGO', 'VOLUNTEER'): User can have either role
 * - isAuthenticated(): Any logged-in user
 *
 * If the check fails, Spring throws AccessDeniedException (403 Forbidden),
 * which is caught by GlobalExceptionHandler.
 *
 * @AuthenticationPrincipal:
 * Injects the currently authenticated UserDetails (our User entity) directly
 * as a method parameter. Avoids calling SecurityContextHolder.getContext().
 */
@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodListingController {

    private final FoodListingService foodListingService;

    /**
     * POST /api/food
     * Create a new food listing. RESTAURANT ONLY.
     */
    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<FoodListingResponse> createListing(
            @Valid @RequestBody FoodListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        FoodListingResponse response = foodListingService.createListing(
                request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/food/available
     * Get all available listings. NGOs and authenticated users.
     */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FoodListingResponse>> getAvailableListings() {
        return ResponseEntity.ok(foodListingService.getAvailableListings());
    }

    /**
     * GET /api/food/my-listings
     * Get listings posted by the authenticated restaurant.
     */
    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<List<FoodListingResponse>> getMyListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getMyListings(userDetails.getUsername()));
    }

    /**
     * PATCH /api/food/{id}/accept
     * NGO accepts a food listing. NGO ONLY.
     */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<FoodListingResponse> acceptListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(foodListingService.acceptListing(id, userDetails.getUsername()));
    }

    /**
     * GET /api/food/ngo-listings
     * Get listings accepted by the authenticated NGO.
     */
    @GetMapping("/ngo-listings")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<FoodListingResponse>> getNgoListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getNgoListings(userDetails.getUsername()));
    }

    /**
     * GET /api/food/active-deliveries
     * Get all active (accepted/in-progress) deliveries. VOLUNTEER only.
     */
    @GetMapping("/active-deliveries")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<FoodListingResponse>> getActiveDeliveries() {
        return ResponseEntity.ok(foodListingService.getActiveDeliveries());
    }

    /**
     * PATCH /api/food/{id}/assign
     * Volunteer assigns themselves to a delivery. VOLUNTEER ONLY.
     */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<FoodListingResponse> assignVolunteer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(foodListingService.assignVolunteer(id, userDetails.getUsername()));
    }

    /**
     * PATCH /api/food/{id}/status
     * Update delivery status (PICKED_UP or DELIVERED). Assigned VOLUNTEER only.
     *
     * Request body: { "status": "PICKED_UP" } or { "status": "DELIVERED" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<FoodListingResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ListingStatus newStatus;
        try {
            newStatus = ListingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                foodListingService.updateDeliveryStatus(id, newStatus, userDetails.getUsername())
        );
    }

    /**
     * GET /api/food/volunteer-listings
     * Get listings assigned to the authenticated volunteer.
     */
    @GetMapping("/volunteer-listings")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<FoodListingResponse>> getVolunteerListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getVolunteerListings(userDetails.getUsername()));
    }
}
