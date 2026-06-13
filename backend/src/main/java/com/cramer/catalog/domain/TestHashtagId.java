package com.cramer.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Composite primary key for {@link TestHashtag} ({@code test_id}, {@code hashtag_id}). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TestHashtagId implements Serializable {

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "hashtag_id")
    private Long hashtagId;
}
