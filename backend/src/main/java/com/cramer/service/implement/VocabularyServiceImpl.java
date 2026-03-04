package com.cramer.service.implement;

import com.cramer.config.LLMConfig;
import com.cramer.dto.VocabularyCreateDTO;
import com.cramer.dto.VocabularyDTO;

import com.cramer.entity.Vocabulary;

import com.cramer.repository.VocabularyRepository;
import com.cramer.service.TranslationBillingService;
import com.cramer.service.VocabularyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.Objects;

/**
 * Implementation of VocabularyService.
 * Provides CRUD operations and AI-powered translation using DeepSeek API.
 */
@Service
public class VocabularyServiceImpl implements VocabularyService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyServiceImpl.class);

    private final VocabularyRepository vocabularyRepository;

    private final TranslationBillingService translationBillingService;
    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VocabularyServiceImpl(
            VocabularyRepository vocabularyRepository,

            TranslationBillingService translationBillingService,
            LLMConfig llmConfig) {
        this.vocabularyRepository = vocabularyRepository;

        this.translationBillingService = translationBillingService;
        this.llmConfig = llmConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyDTO> getAllByUserId(UUID userId) {
        logger.debug("Fetching all vocabulary for user: {}", userId);
        return vocabularyRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(VocabularyDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyDTO> getByUserId(UUID userId, Pageable pageable) {
        logger.debug("Fetching vocabulary page for user: {}", userId);
        return vocabularyRepository.findByUserId(userId, pageable)
                .map(VocabularyDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularyDTO getById(Long id, UUID userId) {
        logger.debug("Fetching vocabulary {} for user: {}", id, userId);
        Vocabulary vocabulary = vocabularyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found or access denied"));
        return VocabularyDTO.fromEntity(vocabulary);
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public VocabularyDTO create(UUID userId, VocabularyCreateDTO createDTO) {
        logger.info("Creating vocabulary entry for user: {}, word: {}", userId, createDTO.getWord());

        // Check for duplicates
        if (vocabularyRepository.existsByUserIdAndWordIgnoreCase(userId, createDTO.getWord())) {
            throw new RuntimeException("Word '" + createDTO.getWord() + "' already exists in your vocabulary");
        }

        Vocabulary vocabulary = Vocabulary.builder()
                .userId(userId)
                .word(createDTO.getWord().trim())
                .translation(createDTO.getTranslation())
                .phonetic(createDTO.getPhonetic())
                .partOfSpeech(createDTO.getPartOfSpeech())
                .definition(createDTO.getDefinition())
                .exampleSentence(createDTO.getExampleSentence())
                .sourceContext(createDTO.getSourceContext())
                .sourceTestId(createDTO.getSourceTestId())
                .sourceSectionId(createDTO.getSourceSectionId())
                .notes(createDTO.getNotes())
                .isMastered(false)
                .reviewCount(0)
                .build();

        // Auto-translate if requested
        if (Boolean.TRUE.equals(createDTO.getAutoTranslate())) {
            try {
                Map<String, String> translation = translateWord(
                        createDTO.getWord(),
                        createDTO.getSourceContext(),
                        userId);

                // Only fill in empty fields
                if (vocabulary.getTranslation() == null || vocabulary.getTranslation().isEmpty()) {
                    vocabulary.setTranslation(translation.get("translation"));
                }
                if (vocabulary.getPhonetic() == null || vocabulary.getPhonetic().isEmpty()) {
                    vocabulary.setPhonetic(translation.get("phonetic"));
                }
                if (vocabulary.getPartOfSpeech() == null || vocabulary.getPartOfSpeech().isEmpty()) {
                    vocabulary.setPartOfSpeech(translation.get("partOfSpeech"));
                }
                if (vocabulary.getDefinition() == null || vocabulary.getDefinition().isEmpty()) {
                    vocabulary.setDefinition(translation.get("definition"));
                }
                if (vocabulary.getExampleSentence() == null || vocabulary.getExampleSentence().isEmpty()) {
                    vocabulary.setExampleSentence(translation.get("exampleSentence"));
                }
            } catch (Exception e) {
                logger.warn("Auto-translation failed for word '{}': {}", createDTO.getWord(), e.getMessage());
                // Continue without translation - user can translate later
            }
        }

        Vocabulary saved = Objects.requireNonNull(vocabularyRepository.save(vocabulary));
        logger.info("Created vocabulary entry with ID: {}", saved.getId());

        return VocabularyDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public VocabularyDTO update(Long id, UUID userId, VocabularyDTO updateDTO) {
        logger.info("Updating vocabulary {} for user: {}", id, userId);

        Vocabulary vocabulary = vocabularyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found or access denied"));

        // Update fields if provided
        if (updateDTO.getWord() != null) {
            // Check for duplicates if word is changing
            if (!vocabulary.getWord().equalsIgnoreCase(updateDTO.getWord()) &&
                    vocabularyRepository.existsByUserIdAndWordIgnoreCase(userId, updateDTO.getWord())) {
                throw new RuntimeException("Word '" + updateDTO.getWord() + "' already exists in your vocabulary");
            }
            vocabulary.setWord(updateDTO.getWord().trim());
        }
        if (updateDTO.getTranslation() != null) {
            vocabulary.setTranslation(updateDTO.getTranslation());
        }
        if (updateDTO.getPhonetic() != null) {
            vocabulary.setPhonetic(updateDTO.getPhonetic());
        }
        if (updateDTO.getPartOfSpeech() != null) {
            vocabulary.setPartOfSpeech(updateDTO.getPartOfSpeech());
        }
        if (updateDTO.getDefinition() != null) {
            vocabulary.setDefinition(updateDTO.getDefinition());
        }
        if (updateDTO.getExampleSentence() != null) {
            vocabulary.setExampleSentence(updateDTO.getExampleSentence());
        }
        if (updateDTO.getSourceContext() != null) {
            vocabulary.setSourceContext(updateDTO.getSourceContext());
        }
        if (updateDTO.getNotes() != null) {
            vocabulary.setNotes(updateDTO.getNotes());
        }

        Vocabulary saved = Objects.requireNonNull(vocabularyRepository.save(vocabulary));
        return VocabularyDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, UUID userId) {
        logger.info("Deleting vocabulary {} for user: {}", id, userId);

        Vocabulary vocabulary = vocabularyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found or access denied"));

        vocabularyRepository.delete(Objects.requireNonNull(vocabulary));
        logger.info("Deleted vocabulary entry: {}", id);
    }

    @Override
    @Transactional
    public VocabularyDTO toggleMastered(Long id, UUID userId) {
        logger.info("Toggling mastered status for vocabulary {} for user: {}", id, userId);

        Vocabulary vocabulary = vocabularyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found or access denied"));

        vocabulary.setIsMastered(!vocabulary.getIsMastered());
        vocabulary.setReviewCount(vocabulary.getReviewCount() + 1);
        vocabulary.setLastReviewedAt(OffsetDateTime.now());

        Vocabulary saved = vocabularyRepository.save(vocabulary);
        return VocabularyDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public Map<String, String> translateWord(String word, String context, UUID userId) {
        logger.info("Translating word: '{}' with context for user: {}", word, userId);

        // Step 1: Check translation quota and process billing
        TranslationBillingService.TranslationBillingResult billingResult = translationBillingService
                .processTranslationBilling(userId);

        if (!billingResult.allowed()) {
            logger.warn("❌ Translation blocked for user {}: {}", userId, billingResult.message());
            throw new RuntimeException(billingResult.message());
        }

        if (billingResult.charged()) {
            logger.info("💰 Charged {} Lúa for translation (overage)", billingResult.luaCost());
        }

        // Step 2: Resolve API key
        String apiKey = resolveApiKey(userId);
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("No DeepSeek API key available. " +
                    "Either set DEEPSEEK_API_KEY environment variable on server, " +
                    "or add your personal API key in Profile settings.");
        }

        String baseUrl = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://api.deepseek.com";
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // Build translation prompt
        String prompt = buildTranslationPrompt(word, context);

        // Use chat model for translation (fast, cost-effective)
        String chatModel = llmConfig.getChatModel() != null ? llmConfig.getChatModel() : "deepseek-chat";
        logger.debug("Using model '{}' for vocabulary translation", chatModel);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", chatModel);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 500);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, Objects.requireNonNull(HttpMethod.POST), entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("DeepSeek API returned status: " + response.getStatusCode());
            }

            return parseTranslationResponse(response.getBody());

        } catch (Exception e) {
            logger.error("Translation API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to translate word: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStats(UUID userId) {
        logger.debug("Fetching vocabulary stats for user: {}", userId);

        long total = vocabularyRepository.countByUserId(userId);
        long mastered = vocabularyRepository.countByUserIdAndIsMastered(userId, true);
        long learning = total - mastered;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("mastered", mastered);
        stats.put("learning", learning);
        stats.put("masteredPercentage", total > 0 ? (mastered * 100.0 / total) : 0.0);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyDTO> search(UUID userId, String searchTerm, Pageable pageable) {
        logger.debug("Searching vocabulary for user: {} with term: {}", userId, searchTerm);
        return vocabularyRepository.searchByWord(userId, searchTerm, pageable)
                .map(VocabularyDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyDTO> searchWithFilter(UUID userId, String searchTerm, Boolean isMastered, Pageable pageable) {
        logger.debug("Searching vocabulary for user: {} with term: {} and mastered: {}", userId, searchTerm,
                isMastered);
        return vocabularyRepository.searchByWordAndMastered(userId, searchTerm, isMastered, pageable)
                .map(VocabularyDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyDTO> getByUserIdAndMastered(UUID userId, Boolean isMastered, Pageable pageable) {
        logger.debug("Fetching vocabulary for user: {} with mastered: {}", userId, isMastered);
        return vocabularyRepository.findByUserIdAndIsMastered(userId, isMastered, pageable)
                .map(VocabularyDTO::fromEntity);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Resolve API key - uses server-side key only.
     * User personal API keys have been deprecated.
     */
    private String resolveApiKey(UUID userId) {
        // Use server-side API key only (user personal keys deprecated)
        if (llmConfig.hasApiKey()) {
            logger.debug("Using server-side API key for translation");
            return llmConfig.getApiKey();
        }

        logger.warn("No server-side API key configured!");
        return null;
    }

    /**
     * Build the translation prompt for DeepSeek.
     */
    private String buildTranslationPrompt(String word, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional English-Vietnamese dictionary assistant. ");
        prompt.append("Your task is to translate English words and provide ENGLISH IPA phonetic transcription.\n\n");

        prompt.append("Translate the English word \"").append(word).append("\" to Vietnamese.\n\n");

        if (context != null && !context.isEmpty()) {
            prompt.append("Context: ").append(context).append("\n\n");
        }

        prompt.append("Provide response in this exact JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"translation\": \"Vietnamese translation\",\n");
        prompt.append("  \"phonetic\": \"/ɪɡˈzæmpəl/\",\n");
        prompt.append("  \"partOfSpeech\": \"noun/verb/adjective/etc\",\n");
        prompt.append("  \"definition\": \"Brief English definition\",\n");
        prompt.append("  \"exampleSentence\": \"An example sentence using the word\"\n");
        prompt.append("}\n\n");

        prompt.append("CRITICAL RULES FOR PHONETIC:\n");
        prompt.append("1. Use ONLY International Phonetic Alphabet (IPA) for English pronunciation\n");
        prompt.append("2. Format: Enclose in forward slashes, e.g., /wɜːd/, /ˈvəʊkæb.jʊ.lər.i/, /ɪɡˈzæm.pəl/\n");
        prompt.append("3. Use proper IPA symbols: ˈ (primary stress), ˌ (secondary stress), ː (long vowel)\n");
        prompt.append("4. Common IPA vowels: /æ/ (cat), /ɑː/ (car), /ɒ/ (lot), /ɔː/ (law), /ʊ/ (put), /uː/ (too)\n");
        prompt.append("5. Common IPA vowels: /ɪ/ (kit), /iː/ (see), /e/ (bed), /ɜː/ (bird), /ə/ (about), /ʌ/ (cup)\n");
        prompt.append(
                "6. Common IPA consonants: /θ/ (think), /ð/ (this), /ʃ/ (she), /ʒ/ (vision), /ŋ/ (sing), /tʃ/ (church), /dʒ/ (judge)\n");
        prompt.append("7. ❌ NEVER use Vietnamese phonetic like \"ê-dăm-pồ\" or \"vô-kép-biu-lơ-ri\"\n");
        prompt.append("8. ❌ NEVER use simplified pronunciations like \"ig-ZAM-pul\" or \"voh-KAB-yuh-lair-ee\"\n\n");

        prompt.append("OTHER RULES:\n");
        prompt.append(
                "- Part of speech: noun, verb, adjective, adverb, preposition, conjunction, pronoun, or interjection\n");
        prompt.append("- Definition: concise, under 100 words, in English\n");
        prompt.append("- Example sentence: natural usage demonstrating the word's meaning\n");

        return prompt.toString();
    }

    /**
     * Parse the translation response from DeepSeek API.
     */
    private Map<String, String> parseTranslationResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Navigate to the content: choices[0].message.content
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Invalid API response: no choices");
            }

            JsonNode messageContent = choices.get(0).get("message").get("content");
            if (messageContent == null) {
                throw new RuntimeException("Invalid API response: no message content");
            }

            String content = messageContent.asText();
            JsonNode translationJson = objectMapper.readTree(content);

            Map<String, String> result = new HashMap<>();
            result.put("translation", getJsonString(translationJson, "translation"));
            result.put("phonetic", getJsonString(translationJson, "phonetic"));
            result.put("partOfSpeech", getJsonString(translationJson, "partOfSpeech"));
            result.put("definition", getJsonString(translationJson, "definition"));
            result.put("exampleSentence", getJsonString(translationJson, "exampleSentence"));

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse translation response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse translation response: " + e.getMessage(), e);
        }
    }

    private String getJsonString(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }
}
