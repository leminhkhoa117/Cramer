package com.cramer.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingTranscriptDTO {

    private Long transcriptId;
    private Long sessionId;
    private Integer turnIndex;
    private String status;
    private OffsetDateTime recordedAt;
}
