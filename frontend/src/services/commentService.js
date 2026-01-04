import axios from 'axios';
import authService from './authService';

const API_URL = 'http://localhost:8081/api/videos';

const getAuthHeader = () => {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const commentService = {
    // Dobavi komentare sa paginacijom
    getComments: async (videoId, page = 0, size = 10) => {
        const response = await axios.get(`${API_URL}/${videoId}/comments?page=${page}&size=${size}`);
        return response.data;
    },

    // Dodaj komentar
    addComment: async (videoId, text) => {
        const response = await axios.post(
            `${API_URL}/${videoId}/comments`,
            { text },
            { headers: getAuthHeader() }
        );
        return response.data;
    },

    // Broj komentara
    getCommentCount: async (videoId) => {
        const response = await axios.get(`${API_URL}/${videoId}/comments/count`);
        return response.data.count;
    },

    // Rate limit status
    getRateLimitStatus: async (videoId) => {
        try {
            const response = await axios.get(
                `${API_URL}/${videoId}/comments/rate-limit`,
                { headers: getAuthHeader() }
            );
            return response.data.remainingComments;
        } catch (error) {
            return null;
        }
    }
};

export default commentService;