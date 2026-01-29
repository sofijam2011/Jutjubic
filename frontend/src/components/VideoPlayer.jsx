import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import './VideoPlayer.css';
import CommentSection from './CommentSection';

const VideoPlayer = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [video, setVideo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [liked, setLiked] = useState(false);
    const [likeCount, setLikeCount] = useState(0);

    const viewCounted = useRef(false);
    const isAuthenticated = !!localStorage.getItem('token');

    const handleBackNavigation = () => {
        if (isAuthenticated) {
            navigate('/dashboard');
        } else {
            navigate('/');
        }
    };

    useEffect(() => {
        console.log('VideoPlayer mounted, authenticated:', isAuthenticated);
        loadVideo();
        loadLikeStatus();
    }, [id]);

    const loadVideo = async () => {
        try {
            console.log('Loading video, ID:', id);
            console.log('Token exists:', !!localStorage.getItem('token'));

            if (!viewCounted.current) {
                console.log('Incrementing view count...');
                await videoService.incrementView(id);
                viewCounted.current = true;
                console.log('View count incremented');
            }

            console.log('Fetching video data...');
            const data = await videoService.getVideoById(id);
            console.log('Video data received:', data);
            setVideo(data);
        } catch (err) {
            console.error('Error loading video:', err);
            console.error('Error details:', err.response?.data);
            console.error('Error status:', err.response?.status);
            setError('Greška pri učitavanju videa: ' + (err.response?.data || err.message));
        } finally {
            setLoading(false);
        }
    };

    const loadLikeStatus = async () => {
        try {
            console.log('Loading like status...');
            const token = localStorage.getItem('token');
            const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

            const response = await fetch(`http://localhost:8081/api/videos/${id}/likes/status`, {
                headers
            });

            console.log('Like status response:', response.status);
            const data = await response.json();
            console.log('Like status data:', data);

            setLiked(data.liked);
            setLikeCount(data.likeCount);
        } catch (err) {
            console.error('Error loading like status:', err);
        }
    };

    const handleLike = async () => {
        if (!isAuthenticated) {
            alert('⚠️ Morate biti prijavljeni da biste lajkovali video!\n\nKliknite "Prijavi se" u meniju.');
            return;
        }

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
            alert('Greška pri lajkovanju videa');
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
                <button onClick={handleBackNavigation} className="btn-back">
                    ← Nazad na početnu
                </button>
            </div>
        );
    }

    console.log('Rendering video player with video:', video);

    return (
        <div className="video-player-container">
            <div className="video-player-nav">
                <button onClick={handleBackNavigation} className="btn-back">
                    ← Nazad
                </button>
            </div>

            <div className="video-player-wrapper">
                <video
                    className="video-element"
                    controls
                    autoPlay
                    poster={`http://localhost:8081/api/videos/${id}/thumbnail`}
                    src={`http://localhost:8081/api/videos/${id}/stream`}
                    onError={(e) => {
                        console.error('Video element error:', e);
                        console.error('Video src:', e.target.src);
                    }}
                    onLoadStart={() => console.log('Video loading started')}
                    onLoadedData={() => console.log('Video data loaded')}
                    onCanPlay={() => console.log('Video can play')}
                >
                    Vaš browser ne podržava video tag.
                </video>
            </div>

            <div className="video-info-section">
                <h1 className="video-title">{video.title}</h1>

                <div className="video-meta">
                    <span
                        className="video-author"
                        onClick={() => navigate(`/user/${video.userId}`)}
                    >
                        @{video.username}
                    </span>
                    <span className="video-views">👁 {video.viewCount} pregleda</span>

                    {video.location && (
                        <span className="video-location">{video.location}</span>
                    )}
                </div>

                <div className="video-actions">
                    <button
                        className={`like-btn ${liked ? 'liked' : ''}`}
                        onClick={handleLike}
                        title={!isAuthenticated ? 'Prijavite se da biste lajkovali' : ''}
                    >
                        {liked ? '❤️' : '🤍'} {likeCount}
                    </button>
                    {!isAuthenticated && (
                        <span className="auth-notice">
                            💡 Prijavite se da biste lajkovali video
                        </span>
                    )}
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

            {/* Sekcija za komentare */}
            <CommentSection videoId={id} />
        </div>
    );
};

export default VideoPlayer;