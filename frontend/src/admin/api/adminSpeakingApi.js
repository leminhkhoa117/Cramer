/**
 * Admin Speaking API Client - API calls cho Admin Speaking Management
 */
import axios from 'axios';
import { supabase } from '../../api/supabaseClient';

// API base URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Get authorization headers with JWT token
 */
const getAuthHeaders = async () => {
    const { data: { session } } = await supabase.auth.getSession();
    if (!session?.access_token) {
        throw new Error('No auth session found');
    }
    return {
        'Authorization': `Bearer ${session.access_token}`,
        'X-User-Id': session.user.id,
        'Content-Type': 'application/json'
    };
};

/**
 * Admin Speaking API
 */
export const adminSpeakingApi = {
    /**
     * Get list of speaking topics
     * @returns {Promise<Array>} List of topics
     */
    getTopics: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/speaking/topics`,
            { headers }
        );
        return response.data;
    }
};

export default adminSpeakingApi;
