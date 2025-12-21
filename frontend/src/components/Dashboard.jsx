import React from 'react';
import { useNavigate } from 'react-router-dom';
import VideoList from './VideoList';
import authService from '../services/authService';
import './Dashboard.css';

const Dashboard = () => {
    const navigate = useNavigate();
    const user = authService.getCurrentUser();

    const handleLogout = () => {
        authService.logout();
        navigate('/login');
    };

    return (
        <div className="dashboard-container">
            {/* Navigation Bar */}
            <nav className="dashboard-navbar">
                <h1
                    className="dashboard-logo"
                    onClick={() => navigate('/dashboard')}
                >
                    🎥 Jutjubic
                </h1>

                <div className="dashboard-nav-links">
                    <button
                        className="nav-btn home-btn"
                        onClick={() => navigate('/dashboard')}
                    >
                        🏠 Početna
                    </button>

                    <span className="nav-username">
                        👤 {user?.username}
                    </span>

                    <button
                        className="nav-btn upload-btn"
                        onClick={() => navigate('/upload')}
                    >
                        🎥 Postavi Video
                    </button>

                    <button
                        className="nav-btn logout-btn"
                        onClick={handleLogout}
                    >
                        🚪 Odjavi se
                    </button>
                </div>
            </nav>

            {/* Video List */}
            <VideoList />
        </div>
    );
};

export default Dashboard;