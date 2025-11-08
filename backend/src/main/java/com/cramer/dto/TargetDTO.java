package com.cramer.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TargetDTO {
    private UUID id;
    private UUID userId;
    private String examName;
    private LocalDate examDate;
    private Double listening;
    private Double reading;
    private Double writing;
    private Double speaking;
}
