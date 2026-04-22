package com.unityeats.repository;

import com.unityeats.model.FoodListing;
import com.unityeats.model.ListingStatus;
import com.unityeats.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FoodListingRepository - Data Access Layer for FoodListing entities.
 *
 * DERIVED QUERY METHOD EXAMPLES:
 * - findByStatus → WHERE status = ?
 * - findByRestaurant → WHERE restaurant_id = ?
 * - findByStatusAndAcceptedByNgo → WHERE status = ? AND ngo_id = ?
 *
 * JPQL QUERY (@Query):
 * Used when derived method names become too complex or unreadable.
 * "SELECT f FROM FoodListing f" uses the ENTITY name, not the table name.
 */
@Repository
public interface FoodListingRepository extends JpaRepository<FoodListing, Long> {

    /**
     * Get all listings with a specific status.
     * Used by NGOs to find AVAILABLE listings.
     */
    List<FoodListing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    /**
     * Get all listings posted by a specific restaurant.
     * Used by the restaurant's dashboard to show their own listings.
     */
    List<FoodListing> findByRestaurantOrderByCreatedAtDesc(User restaurant);

    /**
     * Get listings accepted by a specific NGO.
     * Used by the NGO dashboard.
     */
    List<FoodListing> findByAcceptedByNgoOrderByCreatedAtDesc(User ngo);

    /**
     * Get listings assigned to a specific volunteer.
     * Used by the volunteer dashboard.
     */
    List<FoodListing> findByAssignedVolunteerOrderByCreatedAtDesc(User volunteer);

    /**
     * Get all listings that have been accepted but not yet delivered.
     * Used by volunteers to see what's available for pickup.
     *
     * WHY @Query? The derived method name would be unreadable:
     * findByStatusInOrderByCreatedAtDesc works but is less clear about intent.
     */
    @Query("SELECT f FROM FoodListing f WHERE f.status IN ('ACCEPTED', 'ASSIGNED', 'PICKED_UP') ORDER BY f.createdAt DESC")
    List<FoodListing> findActiveDeliveries();

    /**
     * Count listings by status - used for dashboard statistics.
     */
    long countByStatus(ListingStatus status);
}
