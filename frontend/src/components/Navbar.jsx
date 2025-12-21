import React from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import './Navbar.css';

const Navbar = () => {
    const navigate = useNavigate();
    const user = authService.getCurrentUser();

    const handleLogout = () => {
        authService.logout();
        navigate('/login');
    };

    return (
        <nav className="navbar">
            <h1 className="navbar-logo" onClick={() => navigate('/')}>
                🎥 Jutjubic
            </h1>

            <div className="navbar-links">
                <button
                    className="navbar-btn home"
                    onClick={() => navigate('/')}
                >
                    Početna
                </button>

                {user ? (
                    <>
                        <span className="navbar-user">
                            Dobrodošli, {user.username}!
                        </span>
                        <button
                            className="navbar-btn upload"
                            onClick={() => navigate('/upload')}
                        >
                            🎥 Postavi Video
                        </button>
                        <button
                            className="navbar-btn logout"
                            onClick={handleLogout}
                        >
                            Odjavi se
                        </button>
                    </>
                ) : (
                    <>
                        <button
                            className="navbar-btn"
                            onClick={() => navigate('/login')}
                        >
                            Prijava
                        </button>
                        <button
                            className="navbar-btn"
                            onClick={() => navigate('/register')}
                        >
                            Registracija
                        </button>
                    </>
                )}
            </div>
        </nav>
    );
};

export default Navbar;