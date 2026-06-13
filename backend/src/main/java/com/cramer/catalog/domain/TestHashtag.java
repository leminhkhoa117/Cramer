package com.cramer.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Junction between tests and hashtags, table {@code test_hashtags} (SPEC-11 §1). Carries the
 * {@code is_primary} attribute, so it is modelled as an explicit entity rather than a plain
 * many-to-many.
 */
@Entity
@Table(name = "test_hashtags", schema = "public")
@Getter
@Setter
public class TestHashtag {

    @EmbeddedId
    private TestHashtagId id;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
