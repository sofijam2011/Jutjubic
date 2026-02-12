import axios from 'axios';
import authService from './authService';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/videos`;

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
        const response = await axios.get(API_URL, {
            headers: getAuthHeader()
        });
        return response.data;
    },

    getVideoById: async (id) => {
        const response = await axios.get(`${API_URL}/${id}`, {
            headers: getAuthHeader()
        });
        return response.data;
    },

    incrementView: async (id) => {
        await axios.post(`${API_URL}/${id}/view`, {}, {
            headers: getAuthHeader()
        });
    },

    getThumbnailUrl: (id) => {
        return `${API_URL}/${id}/thumbnail`;
    }
};

export default videoService;