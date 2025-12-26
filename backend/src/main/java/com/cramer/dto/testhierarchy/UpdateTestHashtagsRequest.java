package com.cramer.dto.testhierarchy;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Request DTO for updating hashtags on a test.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestHashtagsRequest {
    
    @NotNull(message = "Hashtag codes list is required")
    private List<String> hashtagCodes;
}
