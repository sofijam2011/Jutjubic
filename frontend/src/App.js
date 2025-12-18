import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import EmailVerification from './components/EmailVerification';
import Dashboard from './components/Dashboard';
import authService from './services/authService';

// Protected Route komponenta - štiti rute koje zahtevaju autentifikaciju
const ProtectedRoute = ({ children }) => {
    const token = authService.getToken();
    return token ? children : <Navigate to="/login" />;
};

function App() {
    return (
        <Router>
            <div className="App">
                <Routes>
                    {/* Početna stranica preusmerava na login */}
                    <Route path="/" element={<Navigate to="/login" />} />

                    {/* Javne rute - dostupne svima */}
                    <Route path="/register" element={<Register />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/verify" element={<EmailVerification />} />

                    {/* Zaštićene rute - samo za prijavljene korisnike */}
                    <Route
                        path="/dashboard"
                        element={
                            <ProtectedRoute>
                                <Dashboard />
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </div>
        </Router>
    );
}

export default App;