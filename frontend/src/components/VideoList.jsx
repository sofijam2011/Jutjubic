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
            console.log('Loaded videos:', data);
            setVideos(data);
        } catch (err) {
            setError('Greška pri učitavanju videa');
            console.error('Error loading videos:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleVideoClick = (videoId) => {
        navigate(`/video/${videoId}`);
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
                    {videos.map(video => {
                        const thumbnailUrl = videoService.getThumbnailUrl(video.id);

                        return (
                            <div
                                key={video.id}
                                className="video-card"
                                onClick={() => handleVideoClick(video.id)}
                            >
                                <div className="video-card-header">
                                    <img
                                        src={thumbnailUrl}
                                        alt={video.title}
                                        className="video-list-thumbnail"
                                        onError={(e) => {
                                            e.target.onerror = null;
                                            e.target.style.display = 'none';
                                            e.target.parentElement.style.backgroundColor = '#1a1a2e';
                                        }}
                                    />
                                </div>

                                <div className="video-info">
                                    <h3>{video.title}</h3>
                                    {video.isScheduled &&
                                     video.scheduledDateTime &&
                                     new Date(video.scheduledDateTime) <= new Date() &&
                                     video.durationSeconds &&
                                     (() => {
                                        const now = new Date();
                                        const scheduledTime = new Date(video.scheduledDateTime);
                                        const elapsedSeconds = Math.floor((now - scheduledTime) / 1000);
                                        return elapsedSeconds < video.durationSeconds;
                                     })() && (
                                        <p className="live-indicator">UŽIVO</p>
                                    )}
                                    <p className="video-author">@{video.username}</p>
                                    <p className="video-views">👁 {video.viewCount} pregleda</p>

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
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default VideoList;
