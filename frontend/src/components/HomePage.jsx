import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import './HomePage.css';

const HomePage = () => {
    const navigate = useNavigate();
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));

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

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setIsAuthenticated(false);
    };

    return (
        <div className="homepage-container">
            <nav className="homepage-navbar">
                <div className="navbar-brand" onClick={() => navigate('/')}>
                    🎬 Jutjubic
                </div>
                <div className="navbar-actions">
                    {isAuthenticated ? (
                        <>
                            <button
                                className="navbar-button upload-button"
                                onClick={() => navigate('/upload')}
                            >
                                📤 Upload Video
                            </button>
                            <button
                                className="navbar-button"
                                onClick={() => navigate('/dashboard')}
                            >
                                👤 Profil
                            </button>
                            <button
                                className="navbar-button logout-button"
                                onClick={handleLogout}
                            >
                                🚪 Odjavi se
                            </button>
                        </>
                    ) : (
                        <>
                            <button
                                className="navbar-button"
                                onClick={() => navigate('/login')}
                            >
                                🔑 Prijavi se
                            </button>
                            <button
                                className="navbar-button register-button"
                                onClick={() => navigate('/register')}
                            >
                                📝 Registruj se
                            </button>
                        </>
                    )}
                </div>
            </nav>

            <div className="homepage-content">
                <div className="welcome-section">
                    <h1>🎥 Dobrodošli na Jutjubic</h1>
                    <p>Pogledajte najnovije videe naše zajednice</p>
                </div>

                {loading ? (
                    <div className="loading">Učitavanje videa...</div>
                ) : videos.length === 0 ? (
                    <div className="no-videos">
                        <p>Još uvek nema objavljenih videa.</p>
                        {isAuthenticated && (
                            <button
                                className="btn-upload"
                                onClick={() => navigate('/upload')}
                            >
                                Budi prvi koji će objaviti video!
                            </button>
                        )}
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
                                        src={`http://localhost:8081/api/videos/${video.id}/thumbnail`}
                                        alt={video.title}
                                    />
                                    <span className="red-dot">●</span>
                                </div>
                                <div className="video-info">
                                    <h3>{video.title}</h3>
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
                                        👁️ {video.viewCount} pregleda • {new Date(video.createdAt).toLocaleDateString('sr-RS')}
                                    </p>
                                    {video.location && (
                                        <p className="video-location">📍 {video.location}</p>
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