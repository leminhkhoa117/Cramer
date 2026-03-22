package com.cramer.service;

import com.cramer.dto.CreateSpeakingSessionDTO;
import com.cramer.dto.PageDTO;
import com.cramer.dto.SaveSpeakingTranscriptDTO;
import com.cramer.dto.SpeakingGradingStatusDTO;
import com.cramer.dto.SpeakingHistoryItemDTO;
import com.cramer.dto.SpeakingResultDTO;
import com.cramer.dto.SpeakingSessionActionDTO;
import com.cramer.dto.SpeakingSessionDTO;
import com.cramer.dto.SpeakingTranscriptDTO;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SpeakingSessionService {

    SpeakingSessionDTO createSession(CreateSpeakingSessionDTO request, UUID userId);

    SpeakingSessionDTO getSession(Long sessionId, UUID userId);

    SpeakingTranscriptDTO saveTranscript(Long sessionId, SaveSpeakingTranscriptDTO request, UUID userId);

    SpeakingSessionActionDTO completeSession(Long sessionId, UUID userId);

    SpeakingSessionActionDTO abandonSession(Long sessionId, UUID userId);

    SpeakingGradingStatusDTO getGradingStatus(Long sessionId, UUID userId);

    SpeakingResultDTO getResults(Long sessionId, UUID userId);

    PageDTO<SpeakingHistoryItemDTO> getHistory(UUID userId, Pageable pageable, String status);
}
