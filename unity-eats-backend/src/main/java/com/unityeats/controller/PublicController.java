package com.unityeats.controller;

import com.unityeats.dto.FoodListingResponse;
import com.unityeats.service.FoodListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * PublicController - Endpoints accessible WITHOUT authentication.
 *
 * WHY A SEPARATE CONTROLLER FOR PUBLIC ENDPOINTS?
 * 1. Clear separation of intent - no @PreAuthorize needed, it's public by design
 * 2. The /api/public/** pattern is whitelisted in SecurityConfig
 * 3. Easier to audit which endpoints are truly public
 *
 * BENEFICIARY USE CASE:
 * Beneficiaries and the general public can view the dashboard and statistics
 * without creating an account. This is intentional for maximum accessibility.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final FoodListingService foodListingService;

    /**
     * GET /api/public/listings
     * Public view of available food listings.
     * Beneficiaries see this on their dashboard.
     */
    @GetMapping("/listings")
    public ResponseEntity<List<FoodListingResponse>> getPublicListings() {
        return ResponseEntity.ok(foodListingService.getAvailableListings());
    }

    /**
     * GET /api/public/stats
     * Platform impact statistics.
     * Shows total donations, deliveries, etc.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(foodListingService.getStatistics());
    }
}
