/**
 * ABTS API Service - Frontend client for AI-Based Test Generation System.
 * 
 * Provides methods to interact with the ABTS backend endpoints.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */

const API_BASE = '/api/admin/abts';

/**
 * Generate Reading content (passage + questions).
 * @param {Object} request - Generation request with topic, facts, difficulty, etc.
 * @returns {Promise<Object>} Generation response with content and metadata.
 */
export async function generateReading(request) {
    return generateContent('reading', request);
}

/**
 * Generate Listening content (transcript + questions).
 * @param {Object} request - Generation request.
 * @returns {Promise<Object>} Generation response.
 */
export async function generateListening(request) {
    return generateContent('listening', request);
}

/**
 * Generate Writing content (Task 1 chart / Task 2 essay prompt).
 * @param {Object} request - Generation request.
 * @returns {Promise<Object>} Generation response.
 */
export async function generateWriting(request) {
    return generateContent('writing', request);
}

/**
 * Regenerate specific questions while keeping existing passage.
 * @param {Object} request - Request with existing passage and question numbers.
 * @returns {Promise<Object>} Generation response with regenerated questions.
 */
export async function regenerateQuestions(request) {
    return generateContent('questions', request);
}

/**
 * Validate generated content against schema and business rules.
 * @param {Object} content - Content to validate.
 * @returns {Promise<Object>} Validation result with errors/warnings.
 */
export async function validateContent(content) {
    const response = await fetch(`${API_BASE}/validate`, {
        method: 'POST',
        headers: await getHeaders(),
        body: JSON.stringify(content)
    });
    return handleResponse(response);
}

/**
 * Save AI-generated content to the database.
 * Creates a new section and all associated questions using the test hierarchy.
 * 
 * @param {Object} params - Save parameters
 * @param {string} [params.examSource] - Exam source identifier (legacy, optional)
 * @param {string} [params.testNumber] - Test number (auto-generated if not provided)
 * @param {string} params.skill - Skill type: "reading", "listening", "writing"
 * @param {number} params.partNumber - Part number (1, 2, 3, etc.)
 * @param {string} [params.topic] - Optional topic name
 * @param {Object} params.content - The GeneratedContentDTO to save
 * @param {number} [params.setId] - Optional: existing TestSet ID
 * @param {string} [params.setCode] - Optional: TestSet code to find or create (default: "ai_generated")
 * @param {number} [params.testId] - Optional: existing Test ID to add section to
 * @param {string[]} [params.hashtagCodes] - Optional: hashtag codes to associate
 * @param {number[]} [params.hashtagIds] - Optional: hashtag IDs to associate
 * @param {Object} [params.generationConfig] - Optional: generation inputs for reproducibility
 * @returns {Promise<Object>} Save result with sectionId, testId, setId and questionsCreated
 */
export async function saveGeneratedTest({
    examSource,
    testNumber,
    skill,
    partNumber,
    topic,
    difficulty,
    content,
    // New hierarchy fields
    setId,
    setCode,
    setName, // Added
    testId,
    testName, // Added
    hashtagCodes,
    hashtagIds,
    generationConfig,
    partsToSave // Added for multi-part support
}) {
    const response = await fetch(`${API_BASE}/save`, {
        method: 'POST',
        headers: await getHeaders(),
        body: JSON.stringify({
            examSource,
            testNumber,
            skill,
            partNumber,
            topic,
            difficulty,
            content,
            // New fields
            setId,
            setCode,
            setNameVi: setName, // Map to backend DTO field
            testId,
            testNameVi: testName, // Map to backend DTO field
            hashtagCodes,
            hashtagIds,
            generationConfig,
            partsToSave // Pass multi-part data
        })
    });
    return handleResponse(response);
}

/**
 * Get all topic template categories.
 * @returns {Promise<Array>} List of categories with id, name, emoji.
 */
export async function getTemplateCategories() {
    const response = await fetch(`${API_BASE}/templates`, {
        headers: await getHeaders()
    });
    return handleResponse(response);
}

/**
 * Get topic templates for a specific category.
 * @param {string} categoryId - Category ID (e.g., 'environment', 'technology').
 * @returns {Promise<Array>} List of templates with facts.
 */
export async function getTemplatesByCategory(categoryId) {
    const response = await fetch(`${API_BASE}/templates/${categoryId}`, {
        headers: await getHeaders()
    });
    return handleResponse(response);
}

/**
 * Get available AI models for ABTS.
 * @returns {Promise<Array>} List of models with id, name, description.
 */
export async function getAvailableModels() {
    const response = await fetch(`${API_BASE}/models`, {
        headers: await getHeaders()
    });
    return handleResponse(response);
}

/**
 * Get ABTS status and configuration.
 * @returns {Promise<Object>} Status with apiKeyConfigured, version, etc.
 */
export async function getStatus() {
    const response = await fetch(`${API_BASE}/status`, {
        headers: await getHeaders()
    });
    return handleResponse(response);
}

// ==================== INTERNAL HELPERS ====================

/**
 * Core generation function.
 */
async function generateContent(skill, request) {
    const response = await fetch(`${API_BASE}/generate/${skill}`, {
        method: 'POST',
        headers: await getHeaders(),
        body: JSON.stringify(request)
    });
    return handleResponse(response);
}

/**
 * Generate Reading content with streaming progress updates.
 * @param {Object} request - Generation request.
 * @param {Object} callbacks - Callback functions for events.
 * @param {Function} callbacks.onProgress - Called with progress updates.
 * @param {Function} callbacks.onComplete - Called when generation completes.
 * @param {Function} callbacks.onError - Called on error.
 * @param {Function} callbacks.onAbort - Called immediately with abort function.
 * @returns {Promise<void>}
 */
export async function generateReadingStream(request, { onProgress, onComplete, onError, onAbort }) {
    const abortController = new AbortController();
    let timeoutId = null;
    let lastEventTime = Date.now();
    let hasWarned = false;

    // IMMEDIATELY provide abort function to caller
    onAbort?.(() => {
        abortController.abort();
    });

    // Send initial event to show connection is being established
    onProgress?.({
        type: 'CONNECTING',
        message: 'Connecting to server...',
        progress: 5
    });

    // Timeout handler - if no events received for 2 minutes, show warning
    const checkTimeout = () => {
        const elapsed = (Date.now() - lastEventTime) / 1000;
        if (elapsed > 120 && !hasWarned) {
            onProgress?.({
                type: 'TIMEOUT_WARNING',
                message: `No response for ${Math.floor(elapsed)}s - model may be slow or unavailable`,
                progress: null
            });
            hasWarned = true;
        }
    };

    try {
        const headers = await getHeaders();

        onProgress?.({
            type: 'SENDING',
            message: 'Request sent, waiting for AI response...',
            progress: 10
        });

        const response = await fetch(`${API_BASE}/generate/reading/stream`, {
            method: 'POST',
            headers,
            body: JSON.stringify(request),
            signal: abortController.signal
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${response.statusText}. ${errorText}`);
        }

        onProgress?.({
            type: 'CONNECTED',
            message: 'Connected! Processing your request...',
            progress: 15
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        // Start timeout checker
        timeoutId = setInterval(checkTimeout, 30000);

        while (true) {
            const { done, value } = await reader.read();

            if (done) {
                break;
            }

            lastEventTime = Date.now();
            hasWarned = false;
            buffer += decoder.decode(value, { stream: true });

            // Parse SSE events from buffer
            const lines = buffer.split('\n');
            buffer = lines.pop() || ''; // Keep incomplete line in buffer

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    try {
                        const data = JSON.parse(line.slice(5).trim());
                        handleStreamEvent(data, { onProgress, onComplete, onError });
                    } catch (e) {
                        // Skip malformed JSON but log it
                        console.warn('Failed to parse SSE data:', line, e);
                    }
                } else if (line.trim()) {
                    // Log non-empty non-data lines for debugging
                    console.debug('SSE line:', line);
                }
            }
        }

        // Stream ended without COMPLETED event
        if (buffer.trim()) {
            console.warn('Unparsed buffer remaining:', buffer);
        }

    } catch (error) {
        if (error.name === 'AbortError') {
            console.log('Stream aborted by user');
            onProgress?.({
                type: 'ABORTED',
                message: 'Generation cancelled',
                progress: 0
            });
        } else {
            console.error('Stream error:', error);
            onError?.(error.message || 'Stream failed');
        }
    } finally {
        if (timeoutId) {
            clearInterval(timeoutId);
        }
    }
}

/**
 * Handle incoming stream events.
 */
function handleStreamEvent(event, { onProgress, onComplete, onError }) {
    switch (event.type) {
        case 'STARTED':
        case 'PROMPT_BUILT':
        case 'AI_CALLING':
        case 'AI_COMPLETED':
        case 'VALIDATING':
        case 'VALIDATION_RESULT':
        case 'RETRY':
        case 'PROGRESS':
            onProgress?.({
                type: event.type,
                message: event.message,
                progress: event.progress,
                attempt: event.attempt,
                maxAttempts: event.maxAttempts,
                partNumber: event.partNumber,
                totalParts: event.totalParts,
                data: event.data
            });
            break;

        // Real-time streaming events - forward reasoning/content tokens
        case 'AI_THINKING':
            onProgress?.({
                type: 'AI_THINKING',
                message: event.message, // Contains reasoning token delta
                isStreaming: true
            });
            break;

        case 'AI_CHUNK':
            onProgress?.({
                type: 'AI_CHUNK',
                data: event.data, // Contains content token delta
                isStreaming: true
            });
            break;

        case 'COMPLETED':
            onComplete?.(event.data);
            break;

        case 'FAILED':
            // FIX 11: surface structured failure data (e.g. per-part errors map) alongside the message.
            onError?.(event.message, event.data);
            break;

        // FIX 2: server emits ABORTED when a generation is cancelled; route it through onError
        // (no dedicated onAbort in handleStreamEvent's callback set).
        case 'ABORTED':
            onError?.('Generation was aborted');
            break;

        default:
            console.log('Unknown stream event:', event.type);
    }
}

/**
 * Generate Listening content with streaming progress updates.
 * @param {Object} request - Generation request.
 * @param {Object} callbacks - Callback functions for events.
 */
export async function generateListeningStream(request, { onProgress, onComplete, onError, onAbort }) {
    return generateStreamInternal('listening', request, { onProgress, onComplete, onError, onAbort });
}

/**
 * Generate Writing content with streaming progress updates.
 * @param {Object} request - Generation request.
 * @param {Object} callbacks - Callback functions for events.
 */
export async function generateWritingStream(request, { onProgress, onComplete, onError, onAbort }) {
    return generateStreamInternal('writing', request, { onProgress, onComplete, onError, onAbort });
}

/**
 * Internal streaming helper - reusable for all skills.
 */
async function generateStreamInternal(skill, request, { onProgress, onComplete, onError, onAbort }) {
    const abortController = new AbortController();
    let timeoutId = null;
    let lastEventTime = Date.now();
    let hasWarned = false;

    // IMMEDIATELY provide abort function to caller
    onAbort?.(() => {
        abortController.abort();
    });

    onProgress?.({
        type: 'CONNECTING',
        message: `Connecting for ${skill} generation...`,
        progress: 5
    });

    const checkTimeout = () => {
        const elapsed = (Date.now() - lastEventTime) / 1000;
        if (elapsed > 120 && !hasWarned) {
            onProgress?.({
                type: 'TIMEOUT_WARNING',
                message: `No response for ${Math.floor(elapsed)}s - model may be slow`,
                progress: null
            });
            hasWarned = true;
        }
    };

    try {
        const headers = await getHeaders();

        onProgress?.({
            type: 'SENDING',
            message: 'Request sent, waiting for AI response...',
            progress: 10
        });

        const response = await fetch(`${API_BASE}/generate/${skill}/stream`, {
            method: 'POST',
            headers,
            body: JSON.stringify(request),
            signal: abortController.signal
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${response.statusText}. ${errorText}`);
        }

        onProgress?.({
            type: 'CONNECTED',
            message: 'Connected! Processing your request...',
            progress: 15
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        timeoutId = setInterval(checkTimeout, 30000);

        while (true) {
            const { done, value } = await reader.read();

            if (done) break;

            lastEventTime = Date.now();
            hasWarned = false;
            buffer += decoder.decode(value, { stream: true });

            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    try {
                        const data = JSON.parse(line.slice(5).trim());
                        handleStreamEvent(data, { onProgress, onComplete, onError });
                    } catch (e) {
                        console.warn('Failed to parse SSE data:', line, e);
                    }
                }
            }
        }

    } catch (error) {
        if (error.name === 'AbortError') {
            onProgress?.({ type: 'ABORTED', message: 'Generation cancelled', progress: 0 });
        } else {
            console.error('Stream error:', error);
            onError?.(error.message || 'Stream failed');
        }
    } finally {
        if (timeoutId) clearInterval(timeoutId);
    }
}

/**
 * Refine content with Agent 2 (streaming).
 * Sends selected issues to be fixed by the refinement agent.
 * @param {Object} request - Refinement request with originalJson, selectedIssueIds, etc.
 * @param {Object} callbacks - Callback functions for events.
 * @param {AbortSignal} signal - Optional abort signal.
 */
export async function refineContentStream(request, { onProgress, onComplete, onError }, signal) {
    let timeoutId = null;
    let lastEventTime = Date.now();

    onProgress?.({
        type: 'CONNECTING',
        message: 'Connecting to refinement agent...',
        progress: 5
    });

    try {
        const headers = await getHeaders();

        const response = await fetch(`${API_BASE}/refine/stream`, {
            method: 'POST',
            headers,
            body: JSON.stringify(request),
            signal
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${response.statusText}. ${errorText}`);
        }

        onProgress?.({
            type: 'CONNECTED',
            message: 'Agent 2 connected, analyzing issues...',
            progress: 15
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        timeoutId = setInterval(() => {
            const elapsed = (Date.now() - lastEventTime) / 1000;
            if (elapsed > 60) {
                onProgress?.({
                    type: 'TIMEOUT_WARNING',
                    message: `Waiting for response (${Math.floor(elapsed)}s)...`
                });
            }
        }, 15000);

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            lastEventTime = Date.now();
            buffer += decoder.decode(value, { stream: true });

            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    try {
                        const data = JSON.parse(line.slice(5).trim());

                        // Handle refinement-specific events
                        if (data.type === 'REFINEMENT_COMPLETED') {
                            onComplete?.(data.data);
                        } else if (data.type === 'FAILED') {
                            onError?.(data.message);
                        } else {
                            onProgress?.(data);
                        }
                    } catch (e) {
                        console.warn('Failed to parse refinement SSE:', line, e);
                    }
                }
            }
        }

    } catch (error) {
        if (error.name === 'AbortError') {
            onProgress?.({ type: 'ABORTED', message: 'Refinement cancelled', progress: 0 });
        } else {
            console.error('Refinement stream error:', error);
            onError?.(error.message || 'Refinement failed');
        }
    } finally {
        if (timeoutId) clearInterval(timeoutId);
    }
}

/**
 * Apply accepted refinement hunks to the original content (per-hunk approval).
 * Calls the backend patch endpoint which returns the patched JSON.
 *
 * @param {Object} payload - { originalJson, hunks, acceptedHunkIds }
 * @returns {Promise<Object>} { patchedJson, appliedCount, rejectedCount, skippedHunkIds, success, errorMessage }
 */
export async function applyRefinementHunks(payload) {
    const response = await fetch(`${API_BASE}/refine/apply`, {
        method: 'POST',
        headers: await getHeaders(),
        body: JSON.stringify(payload)
    });
    return handleResponse(response);
}

/**
 * Get headers with admin user ID and auth token.
 */
async function getHeaders() {
    // Import supabase dynamically to avoid circular dependencies
    const { supabase } = await import('../../api/supabaseClient');
    const { data: { session } } = await supabase.auth.getSession();

    const headers = {
        'Content-Type': 'application/json'
    };

    if (session?.access_token) {
        headers['Authorization'] = `Bearer ${session.access_token}`;
    }

    return headers;
}

/**
 * Handle API response with error parsing.
 */
async function handleResponse(response) {
    // Check if response is empty
    const text = await response.text();

    if (!text || text.trim() === '') {
        const error = new Error('Empty response from server');
        error.status = response.status;
        throw error;
    }

    // Try to parse as JSON
    let data;
    try {
        data = JSON.parse(text);
    } catch (parseError) {
        console.error('Failed to parse response:', text.substring(0, 200));
        const error = new Error(`Invalid JSON response: ${text.substring(0, 100)}`);
        error.status = response.status;
        throw error;
    }

    if (!response.ok) {
        const error = new Error(data.message || data.error || 'ABTS API error');
        error.status = response.status;
        error.data = data;
        throw error;
    }

    return data;
}

// ==================== CONSTANTS ====================

/**
 * Skill types for generation.
 */
export const SKILL_TYPES = {
    READING: 'READING',
    LISTENING: 'LISTENING',
    WRITING: 'WRITING',
    SPEAKING: 'SPEAKING'
};

/**
 * Generation scopes.
 */
export const GENERATION_SCOPES = {
    FULL_SKILL: 'FULL_SKILL',
    SINGLE_PART: 'SINGLE_PART',
    MULTI_PART: 'MULTI_PART',
    QUESTION_GROUP: 'QUESTION_GROUP'
};

/**
 * Difficulty levels mapped to IELTS bands.
 */
export const DIFFICULTY_LEVELS = {
    BEGINNER: { value: 'BEGINNER', label: 'Beginner', bandRange: '4.0-5.0' },
    LOWER_INTERMEDIATE: { value: 'LOWER_INTERMEDIATE', label: 'Lower-Intermediate', bandRange: '5.0-6.0' },
    INTERMEDIATE: { value: 'INTERMEDIATE', label: 'Intermediate', bandRange: '6.0-7.0' },
    UPPER_INTERMEDIATE: { value: 'UPPER_INTERMEDIATE', label: 'Upper-Intermediate', bandRange: '7.0-8.0' },
    ADVANCED: { value: 'ADVANCED', label: 'Advanced/IELTS-like', bandRange: '8.0-9.0' }
};

/**
 * Explanation languages.
 */
export const EXPLANATION_LANGUAGES = {
    VI: { value: 'VI', label: 'Tiếng Việt' },
    EN: { value: 'EN', label: 'English' }
};

/**
 * Test types.
 */
export const TEST_TYPES = {
    ACADEMIC: { value: 'ACADEMIC', label: 'Academic' },
    GENERAL_TRAINING: { value: 'GENERAL_TRAINING', label: 'General Training' }
};

/**
 * Generation status values.
 */
export const GENERATION_STATUS = {
    SUCCESS: 'SUCCESS',
    PARTIAL_SUCCESS: 'PARTIAL_SUCCESS',
    FAILED: 'FAILED'
};

// ==================== DEFAULT EXPORT ====================

export default {
    generateReading,
    generateListening,
    generateWriting,
    regenerateQuestions,
    validateContent,
    saveGeneratedTest,
    refineContentStream,
    applyRefinementHunks,
    getTemplateCategories,
    getTemplatesByCategory,
    getAvailableModels,
    getStatus,
    SKILL_TYPES,
    GENERATION_SCOPES,
    DIFFICULTY_LEVELS,
    EXPLANATION_LANGUAGES,
    TEST_TYPES,
    GENERATION_STATUS
};
