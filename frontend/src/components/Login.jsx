import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import './Auth.css';

const Login = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    });
    const [errors, setErrors] = useState({});
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        const newErrors = {};
        if (!formData.email) {
            newErrors.email = 'Email je obavezan';
        }
        if (!formData.password) {
            newErrors.password = 'Lozinka je obavezna';
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        setLoading(true);
        setMessage('');
        setErrors({});

        try {
            await authService.login(formData);
            navigate('/dashboard');
        } catch (error) {
            if (error.response && error.response.data) {
                if (error.response.data.error) {
                    setMessage(error.response.data.error);
                } else {
                    setErrors(error.response.data);
                }
            } else {
                setMessage('Greška pri prijavi. Pokušajte ponovo.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h2>Prijava</h2>

                {message && (
                    <div className="message error">
                        {message}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Email *</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className={errors.email ? 'error' : ''}
                        />
                        {errors.email && <span className="error-message">{errors.email}</span>}
                    </div>

                    <div className="form-group">
                        <label>Lozinka *</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className={`password-input ${errors.password ? 'error' : ''}`}
                        />
                        {errors.password && <span className="error-message">{errors.password}</span>}
                    </div>

                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? 'Prijava u toku...' : 'Prijavi se'}
                    </button>
                </form>

                <div className="auth-footer">
                    Nemate nalog? <Link to="/register">Registrujte se</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;