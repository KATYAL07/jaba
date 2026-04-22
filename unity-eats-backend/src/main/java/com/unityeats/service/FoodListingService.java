package com.unityeats.service;

import com.unityeats.dto.FoodListingRequest;
import com.unityeats.dto.FoodListingResponse;
import com.unityeats.exception.BusinessException;
import com.unityeats.model.FoodListing;
import com.unityeats.model.ListingStatus;
import com.unityeats.model.Role;
import com.unityeats.model.User;
import com.unityeats.repository.FoodListingRepository;
import com.unityeats.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FoodListingService - Core business logic for the food donation lifecycle.
 *
 * This is the HEART of the Unity Eats platform. Every step of the food
 * redistribution process flows through this service:
 *
 * RESTAURANT posts -> NGO accepts -> VOLUNTEER picks up -> DELIVERED
 *
 * DESIGN PATTERNS USED:
 * 1. Builder Pattern: FoodListing.builder() for constructing entities
 * 2. Guard Clauses: Early returns/throws for invalid state transitions
 * 3. Mapping Pattern: toResponse() converts Entity -> DTO
 * 4. Authorization at Service Level: Verifies the caller is allowed to perform the action
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FoodListingService {

    private final FoodListingRepository listingRepository;
    private final UserRepository userRepository;

    /**
     * Create a new food listing. Only RESTAURANT role can call this.
     * Role check is done at Controller via @PreAuthorize, but we also
     * verify the user exists in the database as a safety net.
     */
    public FoodListingResponse createListing(FoodListingRequest request, String restaurantEmail) {
        User restaurant = getUserByEmail(restaurantEmail);

        // Double-check role in service layer (defense in depth)
        if (restaurant.getRole() != Role.RESTAURANT) {
            throw BusinessException.forbidden("Only restaurants can post food listings.");
        }

        FoodListing listing = FoodListing.builder()
                .foodName(request.getFoodName().trim())
                .category(request.getCategory().trim())
                .quantity(request.getQuantity())
                .unit(request.getUnit().trim())
                .description(request.getDescription())
                .pickupAddress(request.getPickupAddress().trim())
                .expiryHours(request.getExpiryHours())
                .restaurant(restaurant)
                .status(ListingStatus.AVAILABLE)
                .build();

        FoodListing saved = listingRepository.save(listing);
        log.info("New food listing created: '{}' by restaurant '{}'",
                saved.getFoodName(), restaurant.getOrganizationName());

        return toResponse(saved);
    }

    /**
     * Get all AVAILABLE listings - visible to NGOs for accepting.
     * Also visible to public dashboard (beneficiaries).
     */
    @Transactional(readOnly = true)
    public List<FoodListingResponse> getAvailableListings() {
        return listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all listings posted by a specific restaurant.
     */
    @Transactional(readOnly = true)
    public List<FoodListingResponse> getMyListings(String restaurantEmail) {
        User restaurant = getUserByEmail(restaurantEmail);
        return listingRepository.findByRestaurantOrderByCreatedAtDesc(restaurant)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * NGO accepts a food listing.
     *
     * STATE TRANSITION: AVAILABLE -> ACCEPTED
     *
     * BUSINESS RULES:
     * 1. Listing must exist
     * 2. Listing must currently be AVAILABLE (not already accepted by another NGO)
     * 3. The caller must be an NGO
     *
     * WHY CHECK STATUS BEFORE ACCEPTING?
     * In a concurrent system, two NGOs might try to accept the same listing
     * simultaneously. By checking status and updating atomically in a @Transactional
     * method, only one will succeed. The other will see ACCEPTED status.
     */
    public FoodListingResponse acceptListing(Long listingId, String ngoEmail) {
        FoodListing listing = getListingById(listingId);
        User ngo = getUserByEmail(ngoEmail);

        // Guard clause: State must be AVAILABLE
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw BusinessException.conflict(
                "This listing has already been " + listing.getStatus().toString().toLowerCase() +
                " and is no longer available for acceptance."
            );
        }

        // State transition
        listing.setStatus(ListingStatus.ACCEPTED);
        listing.setAcceptedByNgo(ngo);

        FoodListing updated = listingRepository.save(listing);
        log.info("Listing '{}' accepted by NGO '{}'", listing.getFoodName(), ngo.getOrganizationName());

        return toResponse(updated);
    }

    /**
     * Get listings accepted by a specific NGO.
     */
    @Transactional(readOnly = true)
    public List<FoodListingResponse> getNgoListings(String ngoEmail) {
        User ngo = getUserByEmail(ngoEmail);
        return listingRepository.findByAcceptedByNgoOrderByCreatedAtDesc(ngo)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active deliveries (ACCEPTED/ASSIGNED/PICKED_UP).
     * Visible to volunteers to see what's available for pickup.
     */
    @Transactional(readOnly = true)
    public List<FoodListingResponse> getActiveDeliveries() {
        return listingRepository.findActiveDeliveries()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Volunteer assigns themselves to a delivery.
     *
     * STATE TRANSITION: ACCEPTED -> ASSIGNED
     */
    public FoodListingResponse assignVolunteer(Long listingId, String volunteerEmail) {
        FoodListing listing = getListingById(listingId);
        User volunteer = getUserByEmail(volunteerEmail);

        if (listing.getStatus() != ListingStatus.ACCEPTED) {
            throw BusinessException.conflict(
                "This delivery cannot be assigned. Current status: " + listing.getStatus()
            );
        }

        listing.setStatus(ListingStatus.ASSIGNED);
        listing.setAssignedVolunteer(volunteer);

        FoodListing updated = listingRepository.save(listing);
        log.info("Volunteer '{}' assigned to listing '{}'",
                volunteer.getFullName(), listing.getFoodName());

        return toResponse(updated);
    }

    /**
     * Update delivery status (ASSIGNED -> PICKED_UP -> DELIVERED).
     *
     * AUTHORIZATION CHECK:
     * Only the volunteer who is assigned to this delivery can update its status.
     * This prevents other volunteers from hijacking or faking deliveries.
     *
     * STATE MACHINE VALIDATION:
     * We validate the transition is legal before applying it.
     * Can't skip from ASSIGNED directly to DELIVERED.
     */
    public FoodListingResponse updateDeliveryStatus(Long listingId,
                                                     ListingStatus newStatus,
                                                     String volunteerEmail) {
        FoodListing listing = getListingById(listingId);
        User volunteer = getUserByEmail(volunteerEmail);

        // AUTHORIZATION: Only the assigned volunteer can update
        if (listing.getAssignedVolunteer() == null ||
                !listing.getAssignedVolunteer().getEmail().equals(volunteerEmail)) {
            throw BusinessException.forbidden(
                "You are not assigned to this delivery and cannot update its status."
            );
        }

        // VALIDATE STATE MACHINE TRANSITION
        validateStatusTransition(listing.getStatus(), newStatus);

        listing.setStatus(newStatus);
        FoodListing updated = listingRepository.save(listing);

        log.info("Listing '{}' status updated: {} -> {} by volunteer '{}'",
                listing.getFoodName(), listing.getStatus(), newStatus, volunteer.getFullName());

        return toResponse(updated);
    }

    /**
     * Get listings assigned to a specific volunteer.
     */
    @Transactional(readOnly = true)
    public List<FoodListingResponse> getVolunteerListings(String volunteerEmail) {
        User volunteer = getUserByEmail(volunteerEmail);
        return listingRepository.findByAssignedVolunteerOrderByCreatedAtDesc(volunteer)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get platform statistics for the public dashboard.
     * Beneficiaries see these aggregate numbers.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        return Map.of(
                "available", listingRepository.countByStatus(ListingStatus.AVAILABLE),
                "accepted", listingRepository.countByStatus(ListingStatus.ACCEPTED),
                "delivered", listingRepository.countByStatus(ListingStatus.DELIVERED),
                "total", listingRepository.count()
        );
    }

    // ============================================================
    // Private Helper Methods
    // ============================================================

    /**
     * Validate that a status transition is legal according to our state machine.
     *
     * ALLOWED TRANSITIONS:
     * ASSIGNED   -> PICKED_UP
     * PICKED_UP  -> DELIVERED
     *
     * All other transitions are illegal.
     */
    private void validateStatusTransition(ListingStatus current, ListingStatus requested) {
        boolean valid = switch (current) {
            case ASSIGNED -> requested == ListingStatus.PICKED_UP;
            case PICKED_UP -> requested == ListingStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw BusinessException.badRequest(
                "Invalid status transition. Cannot move from " + current + " to " + requested +
                ". Expected: " + (current == ListingStatus.ASSIGNED ? "PICKED_UP" : "DELIVERED")
            );
        }
    }

    /**
     * Load a FoodListing by ID or throw a 404 BusinessException.
     * DRY helper - used in multiple methods.
     */
    private FoodListing getListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                    "Food listing with ID " + id + " was not found."
                ));
    }

    /**
     * Load a User by email or throw a 404 BusinessException.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found: " + email
                ));
    }

    /**
     * Map a FoodListing entity to a FoodListingResponse DTO.
     *
     * WHY A MAPPER METHOD INSTEAD OF A LIBRARY (MapStruct)?
     * For a project this size, a manual mapper is clearer and easier to understand.
     * In larger projects, MapStruct would generate this automatically.
     *
     * LAZY LOADING SAFETY:
     * We're inside a @Transactional method, so LAZY-loaded associations
     * (restaurant, acceptedByNgo, assignedVolunteer) will load when accessed here.
     */
    private FoodListingResponse toResponse(FoodListing listing) {
        return FoodListingResponse.builder()
                .id(listing.getId())
                .foodName(listing.getFoodName())
                .category(listing.getCategory())
                .quantity(listing.getQuantity())
                .unit(listing.getUnit())
                .description(listing.getDescription())
                .pickupAddress(listing.getPickupAddress())
                .expiryHours(listing.getExpiryHours())
                .status(listing.getStatus())
                // Flatten restaurant info
                .restaurantId(listing.getRestaurant().getId())
                .restaurantName(listing.getRestaurant().getFullName())
                .restaurantOrg(listing.getRestaurant().getOrganizationName())
                // Flatten NGO info (null-safe)
                .ngoId(listing.getAcceptedByNgo() != null ? listing.getAcceptedByNgo().getId() : null)
                .ngoName(listing.getAcceptedByNgo() != null ? listing.getAcceptedByNgo().getFullName() : null)
                // Flatten volunteer info (null-safe)
                .volunteerId(listing.getAssignedVolunteer() != null ? listing.getAssignedVolunteer().getId() : null)
                .volunteerName(listing.getAssignedVolunteer() != null ? listing.getAssignedVolunteer().getFullName() : null)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
