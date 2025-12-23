import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import EmailVerification from './components/EmailVerification';
import Dashboard from './components/Dashboard';
import VideoUpload from './components/VideoUpload';
import VideoPlayer from './components/VideoPlayer';
import UserProfile from './components/UserProfile';
import HomePage from './components/HomePage';

const PrivateRoute = ({ children }) => {
    const isAuthenticated = !!localStorage.getItem('token');
    return isAuthenticated ? children : <Navigate to="/login" />;
};

function App() {
    return (
        <Router>
            <div className="App">
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/verify-email" element={<EmailVerification />} />
                    <Route path="/video/:id" element={<VideoPlayer />} />
                    <Route path="/user/:id" element={<UserProfile />} />

                    <Route
                        path="/dashboard"
                        element={
                            <PrivateRoute>
                                <Dashboard />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/upload"
                        element={
                            <PrivateRoute>
                                <VideoUpload />
                            </PrivateRoute>
                        }
                    />
                </Routes>
            </div>
        </Router>
    );
}

export default App;