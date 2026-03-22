package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSpeakingTranscriptDTO {

    @NotNull(message = "sourceQuestionId is required")
    @Min(value = 1, message = "sourceQuestionId must be positive")
    private Long sourceQuestionId;

    @NotNull(message = "partNumber is required")
    @Min(value = 1, message = "partNumber must be positive")
    private Integer partNumber;

    @NotNull(message = "turnIndex is required")
    @Min(value = 1, message = "turnIndex must be positive")
    private Integer turnIndex;

    @NotNull(message = "questionSnapshot is required")
    private JsonNode questionSnapshot;

    @NotBlank(message = "audioStoragePath is required")
    private String audioStoragePath;

    private String transcriptText;

    @Min(value = 0, message = "audioDurationSeconds must be non-negative")
    private Integer audioDurationSeconds;

    @DecimalMin(value = "0.0", message = "transcriptConfidence must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "transcriptConfidence must be between 0 and 1")
    private BigDecimal transcriptConfidence;
}
