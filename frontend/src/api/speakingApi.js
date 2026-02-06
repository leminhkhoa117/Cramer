import axios from 'axios';
import { createClient } from '@supabase/supabase-js';

// Base URL from environment or default to localhost
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Supabase client for storage uploads
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
let supabaseClient = null;

/**
 * Get or create Supabase client for storage operations
 */
const getSupabaseClient = () => {
  if (!supabaseClient && supabaseUrl && supabaseAnonKey) {
    supabaseClient = createClient(supabaseUrl, supabaseAnonKey);
  }
  return supabaseClient;
};

// Create axios instance for speaking APIs
let speakingClient = axios.create({
  baseURL: `${API_BASE_URL}/speaking`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 60000, // 60 second timeout for audio processing
});

// Token provider function
let getAuthToken = () => null;

/**
 * Sets up the token provider function.
 * @param {() => string | null} provider A function that returns the access token.
 */
export const setupSpeakingApiClient = (provider) => {
  getAuthToken = provider;
};

// Request interceptor for auth
speakingClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('❌ Speaking API Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
speakingClient.interceptors.response.use(
  (response) => {
    console.log('✅ Speaking API:', response.config.method?.toUpperCase(), response.config.url, response.status);
    return response;
  },
  (error) => {
    console.error('❌ Speaking API Error:', error.config?.method?.toUpperCase(), error.config?.url);
    console.error('Error details:', {
      message: error.message,
      status: error.response?.status,
      data: error.response?.data
    });
    return Promise.reject(error);
  }
);

// ============================================
// SPEAKING APIs
// ============================================
export const speakingApi = {
  // ========== TOPICS & QUESTIONS ==========

  /**
   * Get all available speaking topics
   * @returns {Promise} Array of topics
   */
  getTopics: () => speakingClient.get('/topics'),

  /**
   * Get questions for a topic and session mode
   * @param {number} topicId - Topic ID
   * @param {string} mode - Session mode: FULL, PART_1, PART_2, PART_3, PART_2_3
   * @returns {Promise} Array of questions
   */
  getQuestions: (topicId, mode) =>
    speakingClient.get('/questions', { params: { topicId, mode } }),

  // ========== SESSION MANAGEMENT ==========

  /**
   * Create a new speaking session
   * @param {string} mode - Session mode
   * @param {number} topicId - Topic ID
   * @returns {Promise} Created session data
   */
  createSession: (mode, topicId) =>
    speakingClient.post('/sessions', { mode, topicId }),

  /**
   * Get session details
   * @param {number} sessionId - Session ID
   * @returns {Promise} Session data
   */
  getSession: (sessionId) =>
    speakingClient.get(`/sessions/${sessionId}`),

  /**
   * Save a transcript for a question
   * @param {number} sessionId - Session ID
   * @param {object} data - Transcript data
   * @param {number} data.questionId - Question ID
   * @param {number} data.part - Part number (1, 2, or 3)
   * @param {string} [data.audioUrl] - URL to audio file in storage
   * @param {number} [data.duration] - Audio duration in seconds
   * @param {string} [data.transcriptText] - Transcript text
   * @returns {Promise} Saved transcript data
   */
  saveTranscript: (sessionId, { questionId, part, audioUrl, duration, transcriptText }) => {
    const params = new URLSearchParams();
    params.append('questionId', questionId);
    params.append('part', part);
    if (audioUrl) params.append('audioUrl', audioUrl);
    if (duration) params.append('duration', duration);

    return speakingClient.post(
      `/sessions/${sessionId}/transcripts?${params.toString()}`,
      transcriptText || '',
      {
        headers: { 'Content-Type': 'text/plain' }
      }
    );
  },

  /**
   * Mark session as complete and trigger AI evaluation
   * @param {number} sessionId - Session ID
   * @returns {Promise} Completion confirmation
   */
  completeSession: (sessionId) =>
    speakingClient.post(`/sessions/${sessionId}/complete`),

  /**
   * Abandon a session (user cancelled)
   * @param {number} sessionId - Session ID
   * @returns {Promise} Abandonment confirmation
   */
  abandonSession: (sessionId) =>
    speakingClient.post(`/sessions/${sessionId}/abandon`),

  // ========== RESULTS ==========

  /**
   * Get evaluation results for a completed session
   * @param {number} sessionId - Session ID
   * @returns {Promise} Evaluation results
   */
  getResults: (sessionId) =>
    speakingClient.get(`/sessions/${sessionId}/results`),

  // ========== HISTORY ==========

  /**
   * Get user's speaking session history
   * @returns {Promise} Array of past sessions
   */
  getHistory: () =>
    speakingClient.get('/history'),

  // ========== AUDIO UPLOAD ==========

  /**
   * Upload audio file to Supabase Storage
   * @param {string} userId - User ID
   * @param {number} sessionId - Session ID
   * @param {number} questionId - Question ID
   * @param {Blob} audioBlob - Audio blob to upload
   * @returns {Promise<string|null>} Public URL of uploaded file or null on error
   */
  uploadAudio: async (userId, sessionId, questionId, audioBlob) => {
    const supabase = getSupabaseClient();
    if (!supabase) {
      console.warn('Supabase client not configured, skipping audio upload');
      return null;
    }

    try {
      // Determine file extension from blob type
      const mimeType = audioBlob.type || 'audio/webm';
      const extension = mimeType.split('/')[1] || 'webm';

      // Create unique filename: userId/sessionId/questionId_timestamp.ext
      const timestamp = Date.now();
      const filePath = `${userId}/${sessionId}/q${questionId}_${timestamp}.${extension}`;

      console.log('Uploading audio to:', filePath, 'Size:', audioBlob.size, 'bytes');

      // Upload to Supabase Storage
      const { data, error } = await supabase.storage
        .from('speaking-audio')
        .upload(filePath, audioBlob, {
          contentType: mimeType,
          upsert: false,
        });

      if (error) {
        console.error('Failed to upload audio:', error);
        return null;
      }

      // Get public URL
      const { data: urlData } = supabase.storage
        .from('speaking-audio')
        .getPublicUrl(filePath);

      console.log('Audio uploaded successfully:', urlData.publicUrl);
      return urlData.publicUrl;
    } catch (err) {
      console.error('Audio upload error:', err);
      return null;
    }
  },

  /**
   * Set Supabase session for authenticated uploads
   * @param {string} accessToken - Supabase access token
   */
  setSupabaseSession: async (accessToken) => {
    const supabase = getSupabaseClient();
    if (supabase && accessToken) {
      await supabase.auth.setSession({
        access_token: accessToken,
        refresh_token: '', // Not needed for storage operations
      });
    }
  },

  // ========== ASR (Speech-to-Text) ==========

  /**
   * Transcribe audio data to text
   * @param {Blob} audioBlob - Audio blob to transcribe
   * @param {string} format - Audio format (default: 'webm')
   * @returns {Promise} Transcription result
   */
  transcribeAudio: (audioBlob, format = 'webm') => {
    const formData = new FormData();
    formData.append('audio', audioBlob, `recording.${format}`);
    formData.append('format', format);

    return speakingClient.post('/transcribe', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 30000, // 30 second timeout for transcription
    });
  },

  /**
   * Transcribe audio from a URL
   * @param {string} audioUrl - Public URL of the audio file
   * @returns {Promise} Transcription result
   */
  transcribeFromUrl: (audioUrl) =>
    speakingClient.post('/transcribe-url', null, { params: { url: audioUrl } }),

  /**
   * Get ASR service status
   * @returns {Promise} ASR configuration status
   */
  getASRStatus: () =>
    speakingClient.get('/asr/status'),

  // ========== FOLLOW-UP QUESTION SELECTION ==========

  /**
   * Select an AI-driven follow-up question based on conversation context
   * @param {Object} params - Request parameters
   * @param {number} params.topicId - Topic ID
   * @param {number} params.part - Current part number (1, 2, or 3)
   * @param {string} params.previousQuestion - Previous question text
   * @param {string} params.candidateAnswer - Candidate's transcript (optional)
   * @param {Array<number>} params.askedQuestionIds - IDs of already asked questions
   * @returns {Promise} Selected follow-up question
   */
  selectFollowUp: ({ topicId, part, previousQuestion, candidateAnswer, askedQuestionIds }) =>
    speakingClient.post('/follow-up', 
      { candidateAnswer, askedQuestionIds },
      { params: { topicId, part, previousQuestion } }
    ),

  /**
   * Check if follow-up AI selection is enabled
   * @returns {Promise} Follow-up status
   */
  getFollowUpStatus: () =>
    speakingClient.get('/follow-up/status'),

  // ========== SAMPLE ANSWER GENERATION ==========

  /**
   * Generate sample answers for questions
   * @param {number} topicId - Topic ID for context
   * @param {Array} questions - Array of question objects
   * @returns {Promise} Map of questionId -> { band6, band8 }
   */
  generateSamples: (topicId, questions) =>
    speakingClient.post('/samples', questions, { params: { topicId } }),

  /**
   * Generate sample answers based on session transcripts
   * @param {number} sessionId - Session ID
   * @returns {Promise} Map of questionId -> { band6, band8 }
   */
  generateSessionSamples: (sessionId) =>
    speakingClient.post(`/sessions/${sessionId}/samples`),

  /**
   * Check if sample generation is enabled
   * @returns {Promise} Sample generation status
   */
  getSampleStatus: () =>
    speakingClient.get('/samples/status'),
};

export default speakingApi;
