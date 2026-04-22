package com.unityeats.controller;

import com.unityeats.model.FoodListing;
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

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodListingController {

    private final FoodListingService foodListingService;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<FoodListing> createListing(
            @Valid @RequestBody FoodListing request,
            @AuthenticationPrincipal UserDetails userDetails) {

        FoodListing response = foodListingService.createListing(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FoodListing>> getAvailableListings() {
        return ResponseEntity.ok(foodListingService.getAvailableListings());
    }

    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<List<FoodListing>> getMyListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getMyListings(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<FoodListing> acceptListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(foodListingService.acceptListing(id, userDetails.getUsername()));
    }

    @GetMapping("/ngo-listings")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<FoodListing>> getNgoListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getNgoListings(userDetails.getUsername()));
    }

    @GetMapping("/active-deliveries")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<FoodListing>> getActiveDeliveries() {
        return ResponseEntity.ok(foodListingService.getActiveDeliveries());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<FoodListing> assignVolunteer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(foodListingService.assignVolunteer(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<FoodListing> updateStatus(
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

        return ResponseEntity.ok(foodListingService.updateDeliveryStatus(id, newStatus, userDetails.getUsername()));
    }

    @GetMapping("/volunteer-listings")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<FoodListing>> getVolunteerListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(foodListingService.getVolunteerListings(userDetails.getUsername()));
    }
}
