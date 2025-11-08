package com.cramer.repository;

import com.cramer.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TargetRepository extends JpaRepository<Target, UUID> {

    /**
     * Finds a target by the user's ID.
     *
     * @param userId The ID of the user.
     * @return an Optional containing the target if found.
     */
    Optional<Target> findByUserId(UUID userId);
}
