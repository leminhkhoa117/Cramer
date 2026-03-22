package com.cramer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSpeakingSessionDTO {

    @NotBlank(message = "sessionMode is required")
    private String sessionMode;

    @NotNull(message = "testId is required")
    @Positive(message = "testId must be positive")
    private Long testId;

    @NotBlank(message = "accent is required")
    private String accent;

    @NotNull(message = "speed is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "speed must be positive")
    private BigDecimal speed;
}
