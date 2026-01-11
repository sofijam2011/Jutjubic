import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './UserProfile.css';

const UserProfile = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadUserProfile();
        loadUserVideos();
    }, [id]);

    const loadUserProfile = async () => {
        try {
            const response = await fetch(`http://localhost:8081/api/users/${id}`);
            if (!response.ok) throw new Error('Korisnik nije pronađen');
            const data = await response.json();
            setProfile(data);
        } catch (err) {
            setError(err.message);
        }
    };

    const loadUserVideos = async () => {
        try {
            const response = await fetch(`http://localhost:8081/api/users/${id}/videos`);
            const data = await response.json();
            setVideos(data);
        } catch (err) {
            console.error('Error loading videos:', err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="user-profile-container">
                <div className="loading">Učitavanje profila...</div>
            </div>
        );
    }

    if (error || !profile) {
        return (
            <div className="user-profile-container">
                <div className="error">{error || 'Profil nije pronađen'}</div>
                <button onClick={() => navigate('/')} className="btn-back">
                    ← Nazad
                </button>
            </div>
        );
    }

    return (
        <div className="user-profile-container">
            <div className="profile-header">
                <button onClick={() => navigate('/')} className="btn-back">
                    ← Nazad
                </button>
            </div>

            <div className="profile-info">
                <div className="profile-avatar">
                    <span className="avatar-placeholder">
                        {profile.firstName[0]}{profile.lastName[0]}
                    </span>
                </div>
                <div className="profile-details">
                    <h1 className="profile-name">
                        {profile.firstName} {profile.lastName}
                    </h1>
                    <p className="profile-username">@{profile.username}</p>
                    <div className="profile-stats">
                        <span className="stat">
                            🎬 {profile.videoCount} {profile.videoCount === 1 ? 'video' : 'videa'}
                        </span>
                        <span className="stat">
                            📅 Član od {new Date(profile.createdAt).toLocaleDateString('sr-RS')}
                        </span>
                    </div>
                </div>
            </div>

            <div className="profile-videos-section">
                <h2>Objavljeni videi</h2>
                {videos.length === 0 ? (
                    <p className="no-videos">Korisnik još nije objavio nijedan video.</p>
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
                                </div>
                                <div className="video-info">
                                    <h3>{video.title}</h3>
                                    <p className="video-meta">
                                        👁️ {video.viewCount} • {new Date(video.createdAt).toLocaleDateString('sr-RS')}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default UserProfile;