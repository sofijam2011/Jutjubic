import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import VideoList from './VideoList';
import authService from '../services/authService';
import popularVideoService from '../services/popularVideoService';
import API_BASE_URL from '../config';
import './Dashboard.css';

const Dashboard = () => {
    const navigate = useNavigate();
    const user = authService.getCurrentUser();
    const [popularVideos, setPopularVideos] = useState([]);
    const [loadingPopular, setLoadingPopular] = useState(true);

    useEffect(() => {
        loadPopularVideos();
    }, []);

    const loadPopularVideos = async () => {
        try {
            const data = await popularVideoService.getPopularVideos();
            setPopularVideos(data);
        } catch (error) {
            console.error('Error loading popular videos:', error);
        } finally {
            setLoadingPopular(false);
        }
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/');
    };


    return (
        <div className="dashboard-container">
            {/* Navigation Bar */}
            <nav className="dashboard-navbar">
                <h1
                    className="dashboard-logo"
                    onClick={() => navigate('/dashboard')}
                >
                    <img
                        src="/harmonika.png"
                        alt="Jutjubic"
                        className="logo-icon"
                        width={45}
                        height={45}
                    />
                    Jutjubic
                </h1>

                <div className="dashboard-nav-links">
                    <span className="nav-username">
                        {user?.username}
                    </span>

                    <button
                        className="nav-btn map-btn"
                        onClick={() => navigate('/map')}
                    >
                        Mapa
                    </button>

                    <button
                        className="nav-btn upload-btn"
                        onClick={() => navigate('/upload')}
                    >
                        Postavi Video
                    </button>

                    <button
                        className="nav-btn logout-btn"
                        onClick={handleLogout}
                    >
                        Odjavi se
                    </button>
                </div>
            </nav>

            {/* Popular Videos Section */}
            {!loadingPopular && popularVideos.length > 0 && (
                <div className="popular-videos-section">
                    <h2 className="section-title">
                        Najpopularniji videi
                    </h2>
                    <div className="popular-videos-grid">
                        {popularVideos.map((video, index) => (
                            <div
                                key={video.id}
                                className="popular-video-card"
                                onClick={() => navigate(`/video/${video.id}`)}
                            >
                                <div className="rank-badge">#{video.rankPosition}</div>
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
                                    <p className="video-author">@{video.username}</p>
                                    <div className="video-stats">
                                        <span>👁 {video.viewCount} pregleda</span>
                                        <span className="popularity-score">
                                            Bodovi: {video.popularityScore.toFixed(1)}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Video List */}
            <VideoList />
        </div>
    );
};

export default Dashboard;