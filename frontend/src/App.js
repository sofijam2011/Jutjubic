import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import EmailVerification from './components/EmailVerification';
import Dashboard from './components/Dashboard';
import VideoUpload from './components/VideoUpload';
import VideoList from './components/VideoList';
import VideoPlayer from './components/VideoPlayer';
import authService from './services/authService';

// Protected Route komponenta - štiti rute koje zahtevaju autentifikaciju
const ProtectedRoute = ({ children }) => {
    const token = authService.getToken();
    return token ? children : <Navigate to="/login" />;
};

// Public Route - redirektuje na dashboard ako je već ulogovan
const PublicRoute = ({ children }) => {
    const token = authService.getToken();
    return token ? <Navigate to="/dashboard" /> : children;
};

function App() {
    return (
        <Router>
            <div className="App">
                <Routes>
                    {/* Početna stranica - redirektuje na login ako nije ulogovan */}
                    <Route
                        path="/"
                        element={
                            authService.getToken()
                                ? <Navigate to="/dashboard" />
                                : <Navigate to="/login" />
                        }
                    />

                    {/* Javne rute - dostupne samo ne-ulogovanim korisnicima */}
                    <Route
                        path="/register"
                        element={
                            <PublicRoute>
                                <Register />
                            </PublicRoute>
                        }
                    />

                    <Route
                        path="/login"
                        element={
                            <PublicRoute>
                                <Login />
                            </PublicRoute>
                        }
                    />

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

                    <Route
                        path="/upload"
                        element={
                            <ProtectedRoute>
                                <VideoUpload />
                            </ProtectedRoute>
                        }
                    />

                    {/* VIDEO PLAYER - Nova ruta za gledanje videa */}
                    <Route
                        path="/watch/:id"
                        element={
                            <ProtectedRoute>
                                <VideoPlayer />
                            </ProtectedRoute>
                        }
                    />

                    {/* Lista videa - dostupna svima */}
                    <Route path="/videos" element={<VideoList />} />

                    {/* Catch all - nepostojeće rute */}
                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;