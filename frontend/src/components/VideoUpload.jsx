import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../services/videoService';
import './VideoUpload.css';

const VideoUpload = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        tags: '',
        location: ''
    });
    const [thumbnail, setThumbnail] = useState(null);
    const [video, setVideo] = useState(null);
    const [progress, setProgress] = useState(0);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleThumbnailChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (!file.type.startsWith('image/')) {
                setError('Molimo izaberite sliku za thumbnail');
                return;
            }
            setThumbnail(file);
            setError('');
        }
    };

    const handleVideoChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.type !== 'video/mp4') {
                setError('Samo MP4 format je dozvoljen');
                return;
            }
            if (file.size > 200 * 1024 * 1024) {
                setError('Video ne može biti veći od 200MB');
                return;
            }
            setVideo(file);
            setError('');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.title.trim()) {
            setError('Naslov je obavezan');
            return;
        }
        if (!thumbnail) {
            setError('Thumbnail slika je obavezna');
            return;
        }
        if (!video) {
            setError('Video fajl je obavezan');
            return;
        }

        setLoading(true);
        setError('');
        setMessage('');
        setProgress(0);

        const uploadData = new FormData();
        uploadData.append('title', formData.title);
        uploadData.append('description', formData.description);


        if (formData.tags.trim()) {
            const tagsArray = formData.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
            tagsArray.forEach(tag => {
                uploadData.append('tags', tag);
            });
        }

        if (formData.location.trim()) {
            uploadData.append('location', formData.location);
        }

        uploadData.append('thumbnail', thumbnail);
        uploadData.append('video', video);

        try {
            await videoService.uploadVideo(uploadData, (progressEvent) => {
                const percentCompleted = Math.round(
                    (progressEvent.loaded * 100) / progressEvent.total
                );
                setProgress(percentCompleted);
            });

            setMessage('Video je uspešno postavljen!');
            setTimeout(() => {
                navigate('/dashboard');
            }, 2000);
        } catch (err) {
            console.error('Upload error:', err);
            setError(err.response?.data?.message || 'Greška pri postavljanju videa. Pokušajte ponovo.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="upload-container">
            <div className="upload-card">
                <h2>Postavi Video</h2>

                {error && <div className="message error">{error}</div>}
                {message && <div className="message success">{message}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Naslov *</label>
                        <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="Unesite naslov videa"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Opis</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Unesite opis videa"
                            rows="4"
                        />
                    </div>

                    <div className="form-group">
                        <label>Tagovi (odvojeni zarezom)</label>
                        <input
                            type="text"
                            name="tags"
                            value={formData.tags}
                            onChange={handleChange}
                            placeholder="muzika, zabava, edukacija"
                        />
                        <small>Primer: sport, muzika, zabava</small>
                    </div>

                    <div className="form-group">
                        <label>Lokacija (opciono)</label>
                        <input
                            type="text"
                            name="location"
                            value={formData.location}
                            onChange={handleChange}
                            placeholder="Beograd, Srbija"
                        />
                    </div>

                    <div className="form-group">
                        <label>Thumbnail Slika *</label>
                        <input
                            type="file"
                            accept="image/*"
                            onChange={handleThumbnailChange}
                            required
                        />
                        {thumbnail && (
                            <small className="file-info">
                                ✓ {thumbnail.name} ({(thumbnail.size / 1024 / 1024).toFixed(2)} MB)
                            </small>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Video Fajl (MP4, max 200MB) *</label>
                        <input
                            type="file"
                            accept="video/mp4"
                            onChange={handleVideoChange}
                            required
                        />
                        {video && (
                            <small className="file-info">
                                ✓ {video.name} ({(video.size / 1024 / 1024).toFixed(2)} MB)
                            </small>
                        )}
                    </div>

                    {loading && (
                        <div className="progress-container">
                            <div className="progress-bar">
                                <div
                                    className="progress-fill"
                                    style={{ width: `${progress}%` }}
                                ></div>
                            </div>
                            <div className="progress-text">{progress}%</div>
                        </div>
                    )}

                    <button
                        type="submit"
                        className="upload-btn"
                        disabled={loading}
                    >
                        {loading ? `Postavljanje... ${progress}%` : 'Postavi Video'}
                    </button>
                </form>

                <div className="auth-footer">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="btn-secondary"
                        disabled={loading}
                    >
                        ← Nazad na početnu
                    </button>
                </div>
            </div>
        </div>
    );
};

export default VideoUpload;