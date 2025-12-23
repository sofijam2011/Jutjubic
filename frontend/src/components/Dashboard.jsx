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

            {/* Video List */}
            <VideoList />
        </div>
    );
};

export default Dashboard;