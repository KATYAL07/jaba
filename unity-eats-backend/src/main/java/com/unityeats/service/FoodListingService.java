package com.unityeats.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FoodListingService {

    private final FoodListingRepository listingRepository;
    private final UserRepository userRepository;

    public FoodListing createListing(FoodListing request, String restaurantEmail) {
        User restaurant = getUserByEmail(restaurantEmail);

        if (restaurant.getRole() != Role.RESTAURANT) {
            throw BusinessException.forbidden("Only restaurants can post food listings.");
        }

        request.setFoodName(request.getFoodName().trim());
        request.setCategory(request.getCategory().trim());
        request.setUnit(request.getUnit().trim());
        request.setPickupAddress(request.getPickupAddress().trim());
        request.setRestaurant(restaurant);
        request.setStatus(ListingStatus.AVAILABLE);

        FoodListing saved = listingRepository.save(request);
        log.info("New food listing created: '{}' by restaurant '{}'", saved.getFoodName(), restaurant.getOrganizationName());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<FoodListing> getAvailableListings() {
        return listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<FoodListing> getMyListings(String restaurantEmail) {
        User restaurant = getUserByEmail(restaurantEmail);
        return listingRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    public FoodListing acceptListing(Long listingId, String ngoEmail) {
        FoodListing listing = getListingById(listingId);
        User ngo = getUserByEmail(ngoEmail);

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw BusinessException.conflict("This listing has already been " + listing.getStatus().toString().toLowerCase() + " and is no longer available.");
        }

        listing.setStatus(ListingStatus.ACCEPTED);
        listing.setAcceptedByNgo(ngo);

        FoodListing updated = listingRepository.save(listing);
        log.info("Listing '{}' accepted by NGO '{}'", listing.getFoodName(), ngo.getOrganizationName());

        return updated;
    }

    @Transactional(readOnly = true)
    public List<FoodListing> getNgoListings(String ngoEmail) {
        User ngo = getUserByEmail(ngoEmail);
        return listingRepository.findByAcceptedByNgoOrderByCreatedAtDesc(ngo);
    }

    @Transactional(readOnly = true)
    public List<FoodListing> getActiveDeliveries() {
        return listingRepository.findActiveDeliveries();
    }

    public FoodListing assignVolunteer(Long listingId, String volunteerEmail) {
        FoodListing listing = getListingById(listingId);
        User volunteer = getUserByEmail(volunteerEmail);

        if (listing.getStatus() != ListingStatus.ACCEPTED) {
            throw BusinessException.conflict("This delivery cannot be assigned. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.ASSIGNED);
        listing.setAssignedVolunteer(volunteer);

        FoodListing updated = listingRepository.save(listing);
        log.info("Volunteer '{}' assigned to listing '{}'", volunteer.getFullName(), listing.getFoodName());

        return updated;
    }

    public FoodListing updateDeliveryStatus(Long listingId, ListingStatus newStatus, String volunteerEmail) {
        FoodListing listing = getListingById(listingId);
        User volunteer = getUserByEmail(volunteerEmail);

        if (listing.getAssignedVolunteer() == null || !listing.getAssignedVolunteer().getEmail().equals(volunteerEmail)) {
            throw BusinessException.forbidden("You are not assigned to this delivery.");
        }

        validateStatusTransition(listing.getStatus(), newStatus);

        listing.setStatus(newStatus);
        FoodListing updated = listingRepository.save(listing);

        log.info("Listing '{}' status updated: {} -> {} by volunteer '{}'", listing.getFoodName(), listing.getStatus(), newStatus, volunteer.getFullName());

        return updated;
    }

    @Transactional(readOnly = true)
    public List<FoodListing> getVolunteerListings(String volunteerEmail) {
        User volunteer = getUserByEmail(volunteerEmail);
        return listingRepository.findByAssignedVolunteerOrderByCreatedAtDesc(volunteer);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        return Map.of(
                "available", listingRepository.countByStatus(ListingStatus.AVAILABLE),
                "accepted", listingRepository.countByStatus(ListingStatus.ACCEPTED),
                "delivered", listingRepository.countByStatus(ListingStatus.DELIVERED),
                "total", listingRepository.count()
        );
    }

    private void validateStatusTransition(ListingStatus current, ListingStatus requested) {
        boolean valid = switch (current) {
            case ASSIGNED -> requested == ListingStatus.PICKED_UP;
            case PICKED_UP -> requested == ListingStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw BusinessException.badRequest("Invalid status transition from " + current + " to " + requested);
        }
    }

    private FoodListing getListingById(Long id) {
        return listingRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Food listing not found."));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
