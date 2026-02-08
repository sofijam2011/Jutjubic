import React, { useState, useEffect } from 'react';
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
    const [isBlocked, setIsBlocked] = useState(false);
    const [remainingTime, setRemainingTime] = useState(0);

    // Proveri localStorage pri učitavanju komponente
    useEffect(() => {
        const blockUntil = localStorage.getItem('loginBlockedUntil');
        if (blockUntil) {
            const blockTime = parseInt(blockUntil);
            const now = Date.now();

            if (now < blockTime) {
                // Još uvek blokiran
                const secondsLeft = Math.ceil((blockTime - now) / 1000);
                setIsBlocked(true);
                setRemainingTime(secondsLeft);
                setMessage('Previše pokušaja prijave. Pokušajte ponovo za 1 minut.');
            } else {
                // Blokada je istekla, očisti localStorage
                localStorage.removeItem('loginBlockedUntil');
            }
        }
    }, []);

    // Countdown timer
    useEffect(() => {
        if (remainingTime > 0) {
            const timer = setTimeout(() => {
                setRemainingTime(remainingTime - 1);
            }, 1000);
            return () => clearTimeout(timer);
        } else if (remainingTime === 0 && isBlocked) {
            // Kada istekne vreme, omogući ponovo i očisti localStorage
            setIsBlocked(false);
            localStorage.removeItem('loginBlockedUntil');
            setMessage('Možete pokušati ponovo sa prijavom.');
        }
    }, [remainingTime, isBlocked]);

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
        // Očisti poruku kada korisnik počne da kuca
        if (message && !isBlocked) {
            setMessage('');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Proveri da li je blokiran
        if (isBlocked) {
            return;
        }

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
            // Uspešna prijava - očisti localStorage ako postoji
            localStorage.removeItem('loginBlockedUntil');
            
            // proveri da li je token zaista sačuvan
            const token = localStorage.getItem('token');
            console.log('Login successful - Token saved:', !!token);
            console.log('Token value:', token ? token.substring(0, 20) + '...' : 'null');
            
            navigate('/dashboard');
        } catch (error) {
            if (error.response && error.response.data) {
                const errorMessage = error.response.data.error;

                // Proveri da li je poruka o blokiranju
                if (errorMessage && errorMessage.includes('Previše pokušaja')) {
                    const blockUntil = Date.now() + (60 * 1000); // Trenutno vreme + 60 sekundi
                    localStorage.setItem('loginBlockedUntil', blockUntil.toString());

                    setIsBlocked(true);
                    setRemainingTime(60);
                    setMessage(errorMessage);
                } else if (errorMessage) {
                    setMessage(errorMessage);
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

    // Formatiranje vremena (MM:SS)
    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h2>Prijava</h2>

                {message && (
                    <div className={`message ${isBlocked ? 'error' : message.includes('Možete') ? 'success' : 'error'}`}>
                        {message}
                        {isBlocked && remainingTime > 0 && (
                            <div className="countdown-timer">
                                Preostalo vreme: <strong>{formatTime(remainingTime)}</strong>
                            </div>
                        )}
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
                            disabled={isBlocked}
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
                            disabled={isBlocked}
                        />
                        {errors.password && <span className="error-message">{errors.password}</span>}
                    </div>

                    <button
                        type="submit"
                        className="btn-primary"
                        disabled={loading || isBlocked}
                    >
                        {isBlocked
                            ? `Blokiran (${formatTime(remainingTime)})`
                            : loading
                                ? 'Prijava u toku...'
                                : 'Prijavi se'
                        }
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
