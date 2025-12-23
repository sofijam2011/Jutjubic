import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import './VideoList.css';

const VideoList = () => {
    const navigate = useNavigate();
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadVideos();
    }, []);

    const loadVideos = async () => {
        try {
            const data = await videoService.getAllVideos();
            setVideos(data);
        } catch (err) {
            setError('Greška pri učitavanju videa');
            console.error('Error loading videos:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleVideoClick = (videoId) => {
        // SAMO navigiraj - NE uvećavaj view count ovde!
        // View count će se uvećati kada se učita VideoPlayer
        navigate(`/watch/${videoId}`);
    };

    if (loading) {
        return (
            <div className="video-list-container">
                <div className="loading">Učitavanje videa...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="video-list-container">
                <div className="error">{error}</div>
            </div>
        );
    }

    return (
        <div className="video-list-container">
            <h2>Najnoviji Videi</h2>

            {videos.length === 0 ? (
                <div className="no-videos">
                    <p>Još nema objavljenih videa.</p>
                </div>
            ) : (
                <div className="video-grid">
                    {videos.map(video => (
                        <div
                            key={video.id}
                            className="video-card"
                            onClick={() => handleVideoClick(video.id)}
                        >
                            <div className="video-card-header">
                                <img
                                    src={videoService.getThumbnailUrl(video.id)}
                                    alt={video.title}
                                    className="video-thumbnail"
                                    onError={(e) => {
                                        e.target.src = 'https://via.placeholder.com/300x200?text=No+Thumbnail';
                                    }}
                                />
                                <div className="live-indicator">●</div>
                            </div>

                            <div className="video-info">
                                <h3>{video.title}</h3>
                                <p className="video-author">@{video.username}</p>
                                <p className="video-views">👁{video.viewCount} pregleda</p>

                                {video.tags && video.tags.length > 0 && (
                                    <div className="video-tags">
                                        {video.tags.map((tag, index) => (
                                            <span key={index} className="tag">#{tag}</span>
                                        ))}
                                    </div>
                                )}

                                {video.description && (
                                    <p className="video-description">
                                        {video.description.length > 100
                                            ? video.description.substring(0, 100) + '...'
                                            : video.description}
                                    </p>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default VideoList;