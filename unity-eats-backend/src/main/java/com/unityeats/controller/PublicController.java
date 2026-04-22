package com.unityeats.controller;

import com.unityeats.model.FoodListing;
import com.unityeats.service.FoodListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final FoodListingService foodListingService;

    @GetMapping("/listings")
    public ResponseEntity<List<FoodListing>> getPublicListings() {
        return ResponseEntity.ok(foodListingService.getAvailableListings());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(foodListingService.getStatistics());
    }
}
