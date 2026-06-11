package com.cramer.identity.repository;

import com.cramer.identity.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository for {@link Profile} (SPEC-10). */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
