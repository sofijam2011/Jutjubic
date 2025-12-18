import React from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

const Dashboard = () => {
    const navigate = useNavigate();
    const user = authService.getCurrentUser();

    const handleLogout = () => {
        authService.logout();
        navigate('/login');
    };

    return (
        <div style={{ padding: '40px', textAlign: 'center' }}>
            <h1>Dobrodošli, {user?.username}!</h1>
            <p>Uspešno ste prijavljeni na sistem.</p>
            <button onClick={handleLogout} style={{
                padding: '10px 20px',
                marginTop: '20px',
                backgroundColor: '#e74c3c',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer'
            }}>
                Odjavi se
            </button>
        </div>
    );
};

export default Dashboard;