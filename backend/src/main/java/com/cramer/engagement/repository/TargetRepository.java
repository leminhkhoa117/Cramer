package com.cramer.engagement.repository;

import com.cramer.engagement.domain.Target;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link Target} (SPEC-16 §5). */
public interface TargetRepository extends JpaRepository<Target, UUID> {

    Optional<Target> findByUserId(UUID userId);
}
