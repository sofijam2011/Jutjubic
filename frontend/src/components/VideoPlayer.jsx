import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import './VideoPlayer.css';

const VideoPlayer = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [video, setVideo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [liked, setLiked] = useState(false);
    const [likeCount, setLikeCount] = useState(0);

    const viewCounted = useRef(false);

    useEffect(() => {
        loadVideo();
        loadLikeStatus();
    }, [id]);

    const loadVideo = async () => {
        try {
            if (!viewCounted.current) {
                await videoService.incrementView(id);
                viewCounted.current = true;
            }

            const data = await videoService.getVideoById(id);
            setVideo(data);
        } catch (err) {
            setError('Greška pri učitavanju videa');
            console.error('Error loading video:', err);
        } finally {
            setLoading(false);
        }
    };

    const loadLikeStatus = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8081/api/videos/${id}/likes/status`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            const data = await response.json();
            setLiked(data.liked);
            setLikeCount(data.likeCount);
        } catch (err) {
            console.error('Error loading like status:', err);
        }
    };

    const handleLike = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`http://localhost:8081/api/videos/${id}/likes`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            const data = await response.json();
            setLiked(data.liked);
            setLikeCount(data.likeCount);
        } catch (err) {
            console.error('Error toggling like:', err);
            alert('Morate biti prijavljeni da biste lajkovali video');
        }
    };

    if (loading) {
        return (
            <div className="video-player-container">
                <div className="loading">Učitavanje videa...</div>
            </div>
        );
    }

    if (error || !video) {
        return (
            <div className="video-player-container">
                <div className="error">{error || 'Video nije pronađen'}</div>
                <button onClick={() => navigate('/dashboard')} className="btn-back">
                    ← Nazad na početnu
                </button>
            </div>
        );
    }

    return (
        <div className="video-player-container">
            <div className="video-player-nav">
                <button onClick={() => navigate('/dashboard')} className="btn-back">
                    ← Nazad
                </button>
            </div>

            <div className="video-player-wrapper">
                <video
                    className="video-element"
                    controls
                    autoPlay
                    src={`http://localhost:8081/api/videos/${id}/stream`}
                >
                    Vaš browser ne podržava video tag.
                </video>
            </div>

            <div className="video-info-section">
                <h1 className="video-title">{video.title}</h1>

                <div className="video-meta">
                    <span className="video-author">@{video.username}</span>
                    <span className="video-views">👁{video.viewCount} pregleda</span>
                    {video.location && (
                        <span className="video-location">{video.location}</span>
                    )}
                </div>

                <div className="video-actions">
                    <button
                        className={`like-btn ${liked ? 'liked' : ''}`}
                        onClick={handleLike}
                    >
                        {liked ? '❤️' : '🤍'} {likeCount}
                    </button>
                </div>

                {video.tags && video.tags.length > 0 && (
                    <div className="video-tags">
                        {video.tags.map((tag, index) => (
                            <span key={index} className="tag">#{tag}</span>
                        ))}
                    </div>
                )}

                {video.description && (
                    <div className="video-description">
                        <h3>Opis:</h3>
                        <p>{video.description}</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default VideoPlayer;