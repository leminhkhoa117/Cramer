package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for updating the hashtags associated with a test.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestHashtagsRequest {
    
    // List of hashtag IDs to associate with the test
    // This replaces all existing hashtag associations
    private List<Long> hashtagIds;
    
    // Optional: ID of the primary hashtag for this test
    // The primary hashtag may be used for display prioritization
    private Long primaryHashtagId;
}
