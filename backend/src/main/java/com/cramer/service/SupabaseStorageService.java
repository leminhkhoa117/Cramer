package com.cramer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for interacting with Supabase Storage.
 * Handles downloading audio files for AI grading.
 */
@Service
public class SupabaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;

    private final RestTemplate restTemplate;

    public SupabaseStorageService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Download a file from Supabase Storage.
     *
     * @param storagePath The path in storage (e.g., "speaking/user-id/session-id/audio.webm")
     * @return byte array of the file content
     * @throws RuntimeException if download fails
     */
    public byte[] download(String storagePath) {
        return download("speaking-audio", storagePath);
    }

    /**
     * Download a file from a specific bucket in Supabase Storage.
     *
     * @param bucket      The storage bucket name
     * @param storagePath The path within the bucket
     * @return byte array of the file content
     * @throws RuntimeException if download fails
     */
    public byte[] download(String bucket, String storagePath) {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            throw new IllegalStateException("Supabase URL not configured");
        }

        if (serviceRoleKey == null || serviceRoleKey.isEmpty()) {
            throw new IllegalStateException("Supabase service role key not configured");
        }

        // Build storage URL
        // Format: {supabase_url}/storage/v1/object/{bucket}/{path}
        String downloadUrl = String.format("%s/storage/v1/object/%s/%s",
            supabaseUrl, bucket, storagePath);

        logger.info("Downloading file from Supabase Storage: {}", storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                entity,
                byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Downloaded {} bytes from {}", response.getBody().length, storagePath);
                return response.getBody();
            }

            throw new RuntimeException("Failed to download file: " + response.getStatusCode());

        } catch (Exception e) {
            logger.error("Failed to download file from Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from a public URL (for audio stored with public access).
     *
     * @param publicUrl The full public URL of the file
     * @return byte array of the file content
     */
    public byte[] downloadFromUrl(String publicUrl) {
        logger.info("Downloading file from URL: {}", publicUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_OCTET_STREAM));

            // Add auth header if it's a Supabase URL
            if (publicUrl.contains("supabase") && serviceRoleKey != null && !serviceRoleKey.isEmpty()) {
                headers.setBearerAuth(serviceRoleKey);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                publicUrl,
                HttpMethod.GET,
                entity,
                byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Downloaded {} bytes from URL", response.getBody().length);
                return response.getBody();
            }

            throw new RuntimeException("Failed to download file: " + response.getStatusCode());

        } catch (Exception e) {
            logger.error("Failed to download file from URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Supabase Storage is configured.
     *
     * @return true if properly configured
     */
    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty()
            && serviceRoleKey != null && !serviceRoleKey.isEmpty();
    }

    /**
     * Upload a file to Supabase Storage.
     *
     * @param bucket      The storage bucket name
     * @param storagePath The path within the bucket
     * @param data        File content as byte array
     * @param contentType MIME type of the file
     * @return Public URL of the uploaded file
     * @throws RuntimeException if upload fails
     */
    public String upload(String bucket, String storagePath, byte[] data, String contentType) {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase Storage not configured");
        }

        // Build upload URL
        // Format: {supabase_url}/storage/v1/object/{bucket}/{path}
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
            supabaseUrl, bucket, storagePath);

        logger.info("Uploading file to Supabase Storage: {} ({} bytes)", storagePath, data.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "true"); // Overwrite if exists

        HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // Build public URL
                String publicUrl = String.format("%s/storage/v1/object/public/%s/%s",
                    supabaseUrl, bucket, storagePath);
                logger.info("Uploaded file successfully: {}", publicUrl);
                return publicUrl;
            }

            throw new RuntimeException("Failed to upload file: " + response.getStatusCode());

        } catch (Exception e) {
            logger.error("Failed to upload file to Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Get the public URL for a file in storage.
     *
     * @param bucket      The storage bucket name
     * @param storagePath The path within the bucket
     * @return Public URL
     */
    public String getPublicUrl(String bucket, String storagePath) {
        return String.format("%s/storage/v1/object/public/%s/%s",
            supabaseUrl, bucket, storagePath);
    }

    /**
     * Check if a file exists in Supabase Storage using HEAD request.
     * This is a lightweight operation that doesn't download the file content.
     *
     * @param bucket      The storage bucket name
     * @param storagePath The path within the bucket
     * @return true if file exists, false otherwise
     */
    public boolean exists(String bucket, String storagePath) {
        if (!isConfigured()) {
            logger.warn("Supabase not configured, cannot check file existence");
            return false;
        }

        // Build storage URL
        // Format: {supabase_url}/storage/v1/object/{bucket}/{path}
        String url = String.format("%s/storage/v1/object/%s/%s",
            supabaseUrl, bucket, storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.HEAD,
                entity,
                Void.class
            );
            boolean exists = response.getStatusCode() == HttpStatus.OK;
            logger.debug("File existence check for {}/{}: {}", bucket, storagePath, exists);
            return exists;

        } catch (Exception e) {
            // 404 = file doesn't exist, other errors = treat as not exists
            logger.debug("File existence check failed for {}/{}: {}", bucket, storagePath, e.getMessage());
            return false;
        }
    }

    /**
     * Extract the audio format from a file path.
     *
     * @param path The file path or URL
     * @return The format string (wav, mp3, webm, ogg)
     */
    public static String extractFormat(String path) {
        if (path == null || path.isEmpty()) {
            return "wav";
        }

        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".wav")) return "wav";
        if (lowerPath.endsWith(".mp3")) return "mp3";
        if (lowerPath.endsWith(".webm")) return "webm";
        if (lowerPath.endsWith(".ogg")) return "ogg";
        if (lowerPath.endsWith(".m4a")) return "m4a";
        if (lowerPath.endsWith(".flac")) return "flac";

        // Default to wav
        return "wav";
    }
}
