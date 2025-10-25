package com.cramer.repository;

import com.cramer.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Profile entity.
 * Provides CRUD operations and custom query methods for user profiles.
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /**
     * Find profile by username.
     * 
     * @param username the username to search for
     * @return Optional containing the profile if found
     */
    Optional<Profile> findByUsername(String username);

    /**
     * Check if a username already exists.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Delete profile by username.
     * 
     * @param username the username of the profile to delete
     */
    void deleteByUsername(String username);
}
