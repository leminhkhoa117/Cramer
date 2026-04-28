package com.cramer.service.implement;

import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.exception.PayloadTooLargeException;
import com.cramer.service.SupabaseStorageService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpeakingAudioPreparer {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingAudioPreparer.class);

    public record PreparedAudio(int turnIndex, String base64Data, String format, int durationSec, String transcodedFrom) {}

    @Value("${speaking.evaluation.per-turn-max-seconds:180}")
    private int perTurnMaxSeconds;

    @Value("${speaking.evaluation.total-payload-cap-mb:18}")
    private int totalPayloadCapMb;

    private final SupabaseStorageService supabaseStorageService;

    public SpeakingAudioPreparer(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    @PostConstruct
    public void checkFfmpeg() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;
            if (exitCode != 0) {
                logger.warn("ffmpeg not available — audio transcoding will fail. Install with: apt-get install ffmpeg");
            } else {
                logger.info("ffmpeg is available for audio transcoding");
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("ffmpeg not available — audio transcoding will fail. Install with: apt-get install ffmpeg");
            Thread.currentThread().interrupt();
        }
    }

    public List<PreparedAudio> prepare(List<SpeakingTranscriptDTO> transcripts) {
        List<PreparedAudio> result = new ArrayList<>();
        long totalBase64Bytes = 0;
        long payloadCapBytes = (long) totalPayloadCapMb * 1024 * 1024;
        int turnsSent = 0;
        int truncatedTurns = 0;
        String transcodedFromSummary = null;

        for (SpeakingTranscriptDTO transcript : transcripts) {
            String audioPath = transcript.getAudioStoragePath();
            if (audioPath == null || audioPath.isBlank()) {
                logger.info("Skipping turn {} — no audio storage path", transcript.getTurnIndex());
                continue;
            }

            byte[] audioBytes;
            try {
                audioBytes = supabaseStorageService.download("speaking-audio", audioPath);
            } catch (Exception e) {
                logger.warn("Failed to download audio for turn {} from {}: {}", transcript.getTurnIndex(), audioPath, e.getMessage());
                continue;
            }

            String format = SupabaseStorageService.extractFormat(audioPath);
            String transcodedFrom = null;

            if (!"mp3".equals(format) && !"wav".equals(format)) {
                Path inputFile = null;
                Path outputFile = null;
                String originalFormat = format;
                try {
                    inputFile = Files.createTempFile("speaking-grading-in-", "." + originalFormat);
                    outputFile = Files.createTempFile("speaking-grading-out-", ".mp3");
                    inputFile.toFile().deleteOnExit();
                    outputFile.toFile().deleteOnExit();

                    Files.write(inputFile, audioBytes);

                    ProcessBuilder pb = new ProcessBuilder(
                            "ffmpeg", "-i", inputFile.toString(),
                            "-ar", "16000", "-ac", "1", "-b:a", "64k",
                            "-f", "mp3", "-y", outputFile.toString());
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    boolean finished = process.waitFor(30, TimeUnit.SECONDS);

                    if (!finished) {
                        process.destroyForcibly();
                        throw new IOException("ffmpeg transcode timed out after 30s for turn " + transcript.getTurnIndex());
                    }
                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        throw new IOException("ffmpeg exit code " + exitCode + " for turn " + transcript.getTurnIndex());
                    }

                    audioBytes = Files.readAllBytes(outputFile);
                    transcodedFrom = originalFormat;
                    format = "mp3";
                    transcodedFromSummary = transcodedFrom;
                } catch (IOException e) {
                    logger.error("Failed to transcode audio for turn {}: {}", transcript.getTurnIndex(), e.getMessage());
                    continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Transcode interrupted for turn {}", transcript.getTurnIndex());
                    continue;
                } finally {
                    if (inputFile != null) {
                        try {
                            Files.deleteIfExists(inputFile);
                        } catch (IOException ignored) {
                        }
                    }
                    if (outputFile != null) {
                        try {
                            Files.deleteIfExists(outputFile);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

            int durationSec;
            if (transcript.getAudioDurationSeconds() != null && transcript.getAudioDurationSeconds() > 0) {
                durationSec = transcript.getAudioDurationSeconds();
            } else {
                durationSec = audioBytes.length / 8000;
            }

            if (durationSec > perTurnMaxSeconds) {
                logger.warn("Skipping turn {} — estimated duration {}s exceeds per-turn max {}s",
                        transcript.getTurnIndex(), durationSec, perTurnMaxSeconds);
                truncatedTurns++;
                continue;
            }

            String base64Data = Base64.getEncoder().encodeToString(audioBytes);
            totalBase64Bytes += base64Data.length();

            if (totalBase64Bytes > payloadCapBytes) {
                logger.error("metric=speaking_audio_payload_too_large totalBytes={} capMb={} turnsIncluded={}",
                        totalBase64Bytes, totalPayloadCapMb, result.size());
                throw new PayloadTooLargeException(
                        String.format("Total audio payload (%d bytes) exceeds cap (%d MB)",
                                totalBase64Bytes, totalPayloadCapMb));
            }

            result.add(new PreparedAudio(transcript.getTurnIndex(), base64Data, format, durationSec, transcodedFrom));
            turnsSent++;
        }

        logger.info("metric=speaking_audio_prepared totalBytes={} turnsSent={} transcodedFrom={} truncatedTurns={}",
                totalBase64Bytes, turnsSent, transcodedFromSummary, truncatedTurns);

        return result;
    }
}
