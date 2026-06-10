import { supabase } from '../../../api/supabaseClient';

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const getAuthHeaders = async () => {
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