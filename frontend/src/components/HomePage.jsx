import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import API_BASE_URL from '../config';
import './HomePage.css';

const HomePage = () => {
    const navigate = useNavigate();
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadVideos();
    }, []);

    const loadVideos = async () => {
        try {
            const data = await videoService.getAllVideos();
            setVideos(data);
        } catch (error) {
            console.error('Error loading videos:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="homepage-container">
            <nav className="homepage-navbar">
                <div className="navbar-brand" onClick={() => navigate('/')}>
                    <img
                        src="/harmonika.png"
                        alt="Jutjubić"
                        className="logo-icon"
                    />
                    Jutjubić
                </div>
                <div className="navbar-actions">
                    <button
                        className="navbar-button map-button"
                        onClick={() => navigate('/monitoring')}
                    >
                        Monitoring
                    </button>
                    <button
                        className="navbar-button map-button"
                        onClick={() => navigate('/map')}
                    >
                        Mapa
                    </button>
                    <button
                        className="navbar-button"
                        onClick={() => navigate('/login')}
                    >
                        Prijavi se
                    </button>
                    <button
                        className="navbar-button register-button"
                        onClick={() => navigate('/register')}
                    >
                        Registruj se
                    </button>
                </div>
            </nav>

            <div className="homepage-content">
                <div className="welcome-section">
                    <h1>Dobro došli na Jutjubić!</h1>
                    <p>Pogledajte najnovije videe naše zajednice.</p>
                </div>

                {loading ? (
                    <div className="loading">Učitavanje videa...</div>
                ) : videos.length === 0 ? (
                    <div className="no-videos">
                        <p>Još uvek nema objavljenih videa.</p>
                    </div>
                ) : (
                    <div className="videos-grid">
                        {videos.map(video => (
                            <div
                                key={video.id}
                                className="video-card"
                                onClick={() => navigate(`/video/${video.id}`)}
                            >
                                <div className="video-thumbnail">
                                    <img
                                        src={`${API_BASE_URL}/api/videos/${video.id}/thumbnail`}
                                        alt={video.title}
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
                                    <p
                                        className="video-author"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            navigate(`/user/${video.userId}`);
                                        }}
                                    >
                                        @{video.username}
                                    </p>
                                    <p className="video-meta">
                                        {video.viewCount} pregleda • {new Date(video.createdAt).toLocaleDateString('sr-RS')}
                                    </p>
                                    {video.location && (
                                        <p className="video-location">{video.location}</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default HomePage;
