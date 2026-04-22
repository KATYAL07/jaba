package com.unityeats.repository;

import com.unityeats.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Data Access Layer for User entities.
 *
 * WHY EXTEND JpaRepository?
 * JpaRepository provides 18+ built-in methods:
 * - findById(), findAll(), save(), delete(), count(), etc.
 * We only need to define custom query methods here.
 *
 * DERIVED QUERY METHODS:
 * Spring Data JPA automatically generates SQL from method names.
 * - findByEmail(email) -> SELECT * FROM users WHERE email = ?
 * - existsByEmail(email) -> SELECT COUNT(*) > 0 FROM users WHERE email = ?
 * No @Query annotation needed!
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email for authentication.
     * Returns Optional<User> - never null, forces null check.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email is already taken during registration.
     * More efficient than findByEmail + isPresent() as it uses COUNT query.
     */
    boolean existsByEmail(String email);
}
