import axios from 'axios';

const API_URL = 'http://localhost:8081/api/popular-videos';

const getPopularVideos = async () => {
    try {
        const response = await axios.get(API_URL);
        return response.data;
    } catch (error) {
        console.error('Error fetching popular videos:', error);
        throw error;
    }
};

const runETLPipeline = async () => {
    try {
        const response = await axios.post(`${API_URL}/run-etl`);
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
