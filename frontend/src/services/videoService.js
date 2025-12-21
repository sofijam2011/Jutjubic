import axios from 'axios';
import authService from './authService';

const API_URL = 'http://localhost:8081/api/videos';

const getAuthHeader = () => {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const videoService = {
    uploadVideo: async (formData, onUploadProgress) => {
        const response = await axios.post(API_URL, formData, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'multipart/form-data'
            },
            onUploadProgress
        });
        return response.data;
    },

    getAllVideos: async () => {
        const response = await axios.get(API_URL);
        return response.data;
    },

    getVideoById: async (id) => {
        const response = await axios.get(`${API_URL}/${id}`);
        return response.data;
    },

    incrementView: async (id) => {
        await axios.post(`${API_URL}/${id}/view`);
    },

    getThumbnailUrl: (id) => {
        return `${API_URL}/${id}/thumbnail`;
    }
};

export default videoService;