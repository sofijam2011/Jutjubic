import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import authService from '../services/authService';
import './Auth.css';

const EmailVerification = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading');
    const [message, setMessage] = useState('');

    useEffect(() => {
        const verifyEmail = async () => {
            const token = searchParams.get('token');

            if (!token) {
                setStatus('error');
                setMessage('Nedostaje verification token');
                return;
            }

            try {
                const response = await authService.verify(token);
                setStatus('success');
                setMessage(response.message);
                setTimeout(() => {
                    navigate('/login');
                }, 3000);
            } catch (error) {
                setStatus('error');
                if (error.response && error.response.data && error.response.data.error) {
                    setMessage(error.response.data.error);
                } else {
                    setMessage('Greška pri verifikaciji naloga');
                }
            }
        };

        verifyEmail();
    }, [searchParams, navigate]);

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h2>Verifikacija emaila</h2>

                {status === 'loading' && (
                    <div className="message">
                        Verifikacija naloga u toku...
                    </div>
                )}

                {status === 'success' && (
                    <div className="message success">
                        {message}
                        <p>Bićete preusmereni na stranicu za prijavu...</p>
                    </div>
                )}

                {status === 'error' && (
                    <div className="message error">
                        {message}
                        <p>
                            <button onClick={() => navigate('/login')} className="btn-primary">
                                Idi na prijavu
                            </button>
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default EmailVerification;