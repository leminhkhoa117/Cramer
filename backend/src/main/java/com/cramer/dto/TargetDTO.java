package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TargetDTO {
    private UUID id;
    private UUID userId;
    private String examName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate examDate;
    private Double listening;
    private Double reading;
    private Double writing;
    private Double speaking;
}
