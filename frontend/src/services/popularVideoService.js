import axios from 'axios';
import authService from './authService';
import API_BASE_URL from '../config';

const API_URL = `${API_BASE_URL}/api/popular-videos`;

const getAuthHeader = () => {
    const token = authService.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

const getPopularVideos = async () => {
    try {
        const response = await axios.get(API_URL, {
            headers: getAuthHeader()
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching popular videos:', error);
        throw error;
    }
};

const runETLPipeline = async () => {
    try {
        const response = await axios.post(`${API_URL}/run-etl`, {}, {
            headers: getAuthHeader()
        });
        return response.data;
    } catch (error) {
        console.error('Error running ETL pipeline:', error);
        throw error;
    }
};

const popularVideoService = {
    getPopularVideos,
    runETLPipeline
};

export default popularVideoService;
