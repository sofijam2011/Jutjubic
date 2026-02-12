import axios from 'axios';
import authService from './authService';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/videos`;

const getAuthHeader = () => {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const commentService = {
    // dobavi komentare sa paginacijom
    getComments: async (videoId, page = 0, size = 10) => {
        const response = await axios.get(`${API_URL}/${videoId}/comments?page=${page}&size=${size}`, {
            headers: getAuthHeader()
        });
        return response.data;
    },

    // dodaj komentar
    addComment: async (videoId, text) => {
        const response = await axios.post(
            `${API_URL}/${videoId}/comments`,
            { text },
            { headers: getAuthHeader() }
        );
        return response.data;
    },

    // broj komentara
    getCommentCount: async (videoId) => {
        const response = await axios.get(`${API_URL}/${videoId}/comments/count`, {
            headers: getAuthHeader()
        });
        return response.data.count;
    },

    // rate limit status
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