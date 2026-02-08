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
    const [streamingInfo, setStreamingInfo] = useState(null);

    const [liked, setLiked] = useState(false);
    const [likeCount, setLikeCount] = useState(0);
    const [currentTime, setCurrentTime] = useState(0);

    const viewCounted = useRef(false);
    const videoRef = useRef(null);
    const syncIntervalRef = useRef(null);
    const preventPauseRef = useRef(false);
    const userInteracted = useRef(false);
    const lastSyncTime = useRef(0);

    const handleBackNavigation = () => {
        if (!!localStorage.getItem('token')) {
            navigate('/dashboard');
        } else {
            navigate('/');
        }
    };

    useEffect(() => {
        const token = localStorage.getItem('token');
        
        loadVideo();
        loadLikeStatus();

        return () => {
            if (syncIntervalRef.current) {
                clearInterval(syncIntervalRef.current);
            }
        };
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

            await loadStreamingInfo();
        } catch (err) {
            console.error('Error loading video:', err);
            console.error('Error details:', err.response?.data);
            console.error('Error status:', err.response?.status);
            setError('Greška pri učitavanju videa: ' + (err.response?.data || err.message));
        } finally {
            setLoading(false);
        }
    };

    const loadStreamingInfo = async () => {
        try {
            const response = await fetch(`http://localhost:8081/api/videos/${id}/streaming-info`);
            const info = await response.json();
            console.log('Streaming info:', info);
            setStreamingInfo(info);
        } catch (err) {
            console.error('Error loading streaming info:', err);
        }
    };

    useEffect(() => {
        if (streamingInfo && streamingInfo.available && streamingInfo.isScheduled && videoRef.current) {
            console.log('Initializing scheduled video with offset:', streamingInfo.offsetSeconds);
            preventPauseRef.current = true;

            const initializeVideo = () => {
                if (videoRef.current) {
                    videoRef.current.currentTime = streamingInfo.offsetSeconds;
                    console.log('Set video currentTime to:', streamingInfo.offsetSeconds);

                    const attemptPlay = (retries = 3) => {
                        videoRef.current?.play().then(() => {
                            console.log('Video playing successfully');
                            userInteracted.current = true;
                        }).catch(err => {
                            console.error('Autoplay error:', err);
                            if (retries > 0) {
                                console.log(`Retrying autoplay... (${retries} attempts left)`);
                                setTimeout(() => attemptPlay(retries - 1), 500);
                            } else {
                                console.warn('Autoplay failed - waiting for user interaction');
                            }
                        });
                    };

                    attemptPlay();
                }
            };

            if (videoRef.current.readyState >= 1) {
                initializeVideo();
            } else {
                videoRef.current.addEventListener('loadedmetadata', initializeVideo, { once: true });
            }

            if (!syncIntervalRef.current) {
                syncIntervalRef.current = setInterval(() => {
                    syncVideoTime();
                }, 10000);
            }
        } else if (streamingInfo && (!streamingInfo.isScheduled || !streamingInfo.available)) {
            preventPauseRef.current = false;
            if (syncIntervalRef.current) {
                clearInterval(syncIntervalRef.current);
                syncIntervalRef.current = null;
            }
        }
    }, [streamingInfo]);

    const syncVideoTime = async () => {
        try {
            const response = await fetch(`http://localhost:8081/api/videos/${id}/streaming-info`);
            const info = await response.json();

            if (info.available && info.isScheduled && videoRef.current) {
                const currentTime = videoRef.current.currentTime;
                const serverTime = info.offsetSeconds;
                const diff = Math.abs(currentTime - serverTime);

                if (videoRef.current.paused) {
                    videoRef.current.play().catch(err => console.error('Play error:', err));
                }


                if (diff > 5) {
                    console.log(`Resyncing: local=${currentTime}s, server=${serverTime}s, diff=${diff}s`);
                    lastSyncTime.current = Date.now();
                    videoRef.current.currentTime = serverTime;
                }
            } else if (info.available && !info.isScheduled && syncIntervalRef.current) {
                clearInterval(syncIntervalRef.current);
                syncIntervalRef.current = null;
                preventPauseRef.current = false;
            }
        } catch (err) {
            console.error('Error syncing video time:', err);
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
        const token = localStorage.getItem('token');
        
        if (!token) {
            alert('⚠️ Morate biti prijavljeni da biste lajkovali video!\n\nKliknite "Prijavi se" u meniju.');
            return;
        }

        try {
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

    const handleVideoClick = () => {
        if (streamingInfo?.isScheduled && videoRef.current) {
            userInteracted.current = true;
            if (videoRef.current.paused) {
                videoRef.current.play().catch(err => console.error('Click play error:', err));
            }
        }
    };

    return (
        <div className="video-player-container">
            <div className="video-player-nav">
                <button onClick={handleBackNavigation} className="btn-back">
                    ← Nazad
                </button>
            </div>

            {streamingInfo && streamingInfo.isScheduled && (
                <div className="streaming-notice">
                    UŽIVO: Video je u zakazanom režimu. Svi gledalci gledaju sinhronizovano i ne mogu pauzirati.
                    {videoRef.current?.paused && !userInteracted.current && (
                        <div style={{ marginTop: '5px', fontSize: '0.9em' }}>
                            Kliknite na video da bi se pustio
                        </div>
                    )}
                </div>
            )}

            <div className="video-player-wrapper" onClick={handleVideoClick}>
                {streamingInfo?.isScheduled && (
                    <div className="video-time-overlay">
                        {Math.floor(currentTime / 60)}:{(Math.floor(currentTime) % 60).toString().padStart(2, '0')}
                    </div>
                )}
                <video
                    className={`video-element ${streamingInfo?.isScheduled ? 'live-mode' : ''}`}
                    controls={!streamingInfo?.isScheduled}
                    autoPlay={!streamingInfo?.isScheduled}
                    poster={`http://localhost:8081/api/videos/${id}/thumbnail`}
                    ref={videoRef}
                    src={`http://localhost:8081/api/videos/${id}/stream`}
                    onError={(e) => {
                        console.error('Video element error:', e);
                        console.error('Video src:', e.target.src);
                    }}
                    onLoadStart={() => console.log('Video loading started')}
                    onLoadedData={() => console.log('Video data loaded')}
                    onTimeUpdate={(e) => {
                        setCurrentTime(e.target.currentTime);
                    }}
                    onCanPlay={() => {
                        console.log('Video can play');
                        if (streamingInfo?.isScheduled && videoRef.current?.paused) {
                            videoRef.current.play().catch(err => console.error('Play on canplay error:', err));
                        }
                    }}
                    onSeeking={(e) => {
                        if (streamingInfo && streamingInfo.isScheduled && !streamingInfo.canSeek) {
                            const targetTime = e.target.currentTime;
                            const expectedTime = streamingInfo.offsetSeconds;

                            if (Math.abs(targetTime - expectedTime) > 10) {
                                e.preventDefault();
                                console.log('Blocking seek attempt');
                            }
                        }
                    }}
                    onPause={(e) => {
                        if (preventPauseRef.current && streamingInfo?.isScheduled) {
                            e.preventDefault();
                            videoRef.current.play().catch(err => console.error('Play error:', err));
                        }
                    }}
                    onPlay={() => {
                        console.log('Video playing');
                    }}
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
                        title={!localStorage.getItem('token') ? 'Prijavite se da biste lajkovali' : ''}
                    >
                        {liked ? '❤️' : '🤍'} {likeCount}
                    </button>
                    {!localStorage.getItem('token') && (
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