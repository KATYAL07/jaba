package com.unityeats.config;

import com.unityeats.model.*;
import com.unityeats.repository.FoodListingRepository;
import com.unityeats.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * DataSeeder - Populates the H2 in-memory database with demo data on startup.
 *
 * WHY CommandLineRunner?
 * CommandLineRunner is a Spring Boot functional interface. When you define a
 * @Bean of type CommandLineRunner, Spring Boot runs it automatically AFTER
 * the application context is fully loaded (all beans initialized, DB schema created).
 *
 * This is the ideal place for data seeding because:
 * 1. The JPA schema (tables) is already created by Hibernate
 * 2. All beans (UserRepository, PasswordEncoder) are available for injection
 * 3. It's wrapped in a try-catch so a seeding failure doesn't crash the app
 *
 * DEMO CREDENTIALS (for quick testing):
 * Restaurant: restaurant@demo.com / Demo@1234
 * NGO:        ngo@demo.com        / Demo@1234
 * Volunteer:  volunteer@demo.com  / Demo@1234
 * Beneficiary: beneficiary@demo.com / Demo@1234
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final FoodListingRepository listingRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            // Only seed if the database is empty (prevents duplicate data on restart)
            if (userRepository.count() > 0) {
                log.info("Database already seeded. Skipping data seeding.");
                return;
            }

            log.info("Seeding demo data...");

            // --------------------------------------------------------
            // Create Demo Users
            // --------------------------------------------------------
            String encodedPassword = passwordEncoder.encode("Demo@1234");

            User restaurant = userRepository.save(User.builder()
                    .fullName("Arnav Katyal")
                    .email("restaurant@demo.com")
                    .phone("+919876543210")
                    .password(encodedPassword)
                    .role(Role.RESTAURANT)
                    .organizationName("The Grand Biryani House")
                    .build());

            User restaurant2 = userRepository.save(User.builder()
                    .fullName("Ayush Singh")
                    .email("restaurant2@demo.com")
                    .phone("+919123456789")
                    .password(encodedPassword)
                    .role(Role.RESTAURANT)
                    .organizationName("Sunrise Bakery and Cafe")
                    .build());

            User ngo = userRepository.save(User.builder()
                    .fullName("Arnav Katyal")
                    .email("ngo@demo.com")
                    .phone("+918765432109")
                    .password(encodedPassword)
                    .role(Role.NGO)
                    .organizationName("Feeding Futures Foundation")
                    .build());

            User volunteer = userRepository.save(User.builder()
                    .fullName("Ayush Singh")
                    .email("volunteer@demo.com")
                    .phone("+917654321098")
                    .password(encodedPassword)
                    .role(Role.VOLUNTEER)
                    .organizationName(null)
                    .build());

            userRepository.save(User.builder()
                    .fullName("Arnav Katyal")
                    .email("beneficiary@demo.com")
                    .phone("+916543210987")
                    .password(encodedPassword)
                    .role(Role.BENEFICIARY)
                    .organizationName(null)
                    .build());

            // --------------------------------------------------------
            // Create Demo Food Listings
            // --------------------------------------------------------

            // Listing 1: Available (can be accepted by NGO)
            FoodListing biryani = listingRepository.save(FoodListing.builder()
                    .foodName("Chicken Biryani")
                    .category("Cooked Meals")
                    .quantity(50)
                    .unit("servings")
                    .description("Freshly prepared Hyderabadi Dum Biryani with raita. Made for a wedding event.")
                    .pickupAddress("Connaught Place, Delhi, India 110058")
                    .expiryHours(6)
                    .restaurant(restaurant)
                    .status(ListingStatus.AVAILABLE)
                    .build());

            FoodListing bread = listingRepository.save(FoodListing.builder()
                    .foodName("Assorted Bread Loaves")
                    .category("Bakery")
                    .quantity(30)
                    .unit("loaves")
                    .description("Freshly baked whole wheat and multigrain loaves from morning batch.")
                    .pickupAddress("Hauz Khas, Delhi, India 110058")
                    .expiryHours(12)
                    .restaurant(restaurant2)
                    .status(ListingStatus.AVAILABLE)
                    .build());

            FoodListing paneer = listingRepository.save(FoodListing.builder()
                    .foodName("Paneer Butter Masala")
                    .category("Cooked Meals")
                    .quantity(25)
                    .unit("servings")
                    .description("Rich and creamy paneer curry with naan bread.")
                    .pickupAddress("Karol Bagh, Delhi, India 110058")
                    .expiryHours(4)
                    .restaurant(restaurant)
                    .status(ListingStatus.AVAILABLE)
                    .build());

            // Listing 2: Accepted by NGO
            FoodListing milk = listingRepository.save(FoodListing.builder()
                    .foodName("Fresh Milk")
                    .category("Dairy")
                    .quantity(100)
                    .unit("liters")
                    .description("Full cream fresh milk, collected this morning.")
                    .pickupAddress("Lajpat Nagar, Delhi, India 110058")
                    .expiryHours(8)
                    .restaurant(restaurant2)
                    .status(ListingStatus.ACCEPTED)
                    .acceptedByNgo(ngo)
                    .build());

            // Listing 3: Assigned to volunteer and delivered
            FoodListing rice = listingRepository.save(FoodListing.builder()
                    .foodName("Steamed Rice")
                    .category("Cooked Meals")
                    .quantity(80)
                    .unit("servings")
                    .description("Plain steamed basmati rice.")
                    .pickupAddress("Dwarka, Delhi, India 110058")
                    .expiryHours(3)
                    .restaurant(restaurant)
                    .status(ListingStatus.DELIVERED)
                    .acceptedByNgo(ngo)
                    .assignedVolunteer(volunteer)
                    .build());

            log.info("✅ Demo data seeded successfully!");
            log.info("   Users created: restaurant@demo.com, ngo@demo.com, volunteer@demo.com, beneficiary@demo.com");
            log.info("   Password for all: Demo@1234");
            log.info("   Food listings created: {}", listingRepository.count());
        };
    }
}
