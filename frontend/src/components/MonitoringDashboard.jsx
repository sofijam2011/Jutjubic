import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './MonitoringDashboard.css';

const MonitoringDashboard = () => {
    const navigate = useNavigate();
    const [grafanaReady, setGrafanaReady] = useState(true); // Pretpostavi da je dostupna
    const [activeTab, setActiveTab] = useState('overview');

    useEffect(() => {
        // Ne proveravamo više, pretpostavljamo da je dostupna
        // Korisnik će videti error ako nije pokrenuta
    }, []);

    // Panel IDs iz Grafana dashboarda
    const panels = {
        connections: 1,  // Database Connection Pool - Active & Idle
        cpuGauge: 2,     // System CPU Usage 
        cpuAvg: 3,       // Average CPU Usage Over Time
        activeUsers: 4,  // Active Users Count
        currentUsers: 5, // Current Active Users
        activeConn: 6,   // Active DB Connections
        idleConn: 7,     // Idle DB Connections
    };

    const grafanaBaseUrl = 'http://localhost:3001';
    const dashboardUid = 'jutjubic-dashboard';
    const refreshParam = '&refresh=1s'; // Auto-refresh svakih 1 sekund

    return (
        <div className="monitoring-container">
            {/* Navbar */}
            <nav className="monitoring-navbar">
                <div className="navbar-brand" onClick={() => navigate('/')}>
                    <img src="/harmonika.png" alt="Jutjubić" className="logo-icon" />
                    Jutjubić - Monitoring
                </div>
                <div className="navbar-actions">
                    <button className="navbar-button" onClick={() => navigate('/')}>
                        ← Nazad na početnu
                    </button>
                </div>
            </nav>

            {/* Header */}
            <div className="monitoring-header">
                <h1>Praćenje Aktivnosti i Performansi Aplikacije</h1>
                <p className="monitoring-subtitle">
                    Real-time monitoring pomoću Prometheus i Grafana
                </p>
            </div>

            {/* Status Check - removed, assuming Grafana is running */}

            {/* Tabs */}
            <div className="monitoring-tabs">
                <button 
                    className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
                    onClick={() => setActiveTab('overview')}
                >
                    Pregled
                </button>
                <button 
                    className={`tab-button ${activeTab === 'database' ? 'active' : ''}`}
                    onClick={() => setActiveTab('database')}
                >
                    Baza Podataka
                </button>
                <button 
                    className={`tab-button ${activeTab === 'cpu' ? 'active' : ''}`}
                    onClick={() => setActiveTab('cpu')}
                >
                    CPU
                </button>
                <button 
                    className={`tab-button ${activeTab === 'users' ? 'active' : ''}`}
                    onClick={() => setActiveTab('users')}
                >
                    Aktivni Korisnici
                </button>
                <button 
                    className={`tab-button ${activeTab === 'full' ? 'active' : ''}`}
                    onClick={() => setActiveTab('full')}
                >
                    Kompletan Dashboard
                </button>
            </div>

            {/* Content */}
            <div className="monitoring-content">
                {activeTab === 'overview' && (
                    <div className="overview-section">
                        <div className="metric-cards">
                            <div className="metric-card">
                                <h3>Konekcije ka Bazi</h3>
                                <p>Praćenje aktivnih i idle konekcija ka PostgreSQL bazi</p>
                                <div className="metric-info">
                                    <span className="info-label">Kada:</span>
                                    <span>Veliko opterećenje (200+ req/s)</span>
                                </div>
                            </div>

                            <div className="metric-card">
                                <h3>Zauzeće CPU</h3>
                                <p>Prosečno CPU zauzeće u različitim vremenskim intervalima</p>
                                <div className="metric-info">
                                    <span className="info-label">Intervali:</span>
                                    <span>5 min, 15 min, 1h</span>
                                </div>
                            </div>

                            <div className="metric-card">
                                <h3>Aktivni Korisnici</h3>
                                <p>Broj trenutno aktivnih korisnika u toku 24h</p>
                                <div className="metric-info">
                                    <span className="info-label">Aktivan:</span>
                                    <span>Ako je bio aktivan u poslednjih 5 min</span>
                                </div>
                            </div>
                        </div>

                        <div className="info-section">
                            <h2>Informacije</h2>
                            <div className="info-content">
                                <div className="info-item">
                                    <strong>Prometheus:</strong> Prikuplja metrike sa aplikacije svakih 5 sekundi
                                </div>
                                <div className="info-item">
                                    <strong>Grafana:</strong> Vizualizuje metrike kroz interaktivne grafikone
                                </div>
                                <div className="info-item">
                                    <strong>HikariCP:</strong> Connection pool koji upravlja konekcijama ka bazi
                                </div>
                                <div className="info-item">
                                    <strong>Actuator:</strong> Spring Boot endpoint koji eksponuje metrike na /actuator/prometheus
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {activeTab === 'database' && (
                    <div className="panels-section">
                        <h2>Konekcije ka Bazi Podataka</h2>
                        
                        <div className="panels-grid">
                            {/* Connection Pool Graph */}
                            <div className="panel-wrapper full-width">
                                <h3>Aktivne i Idle Konekcije (Grafik)</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.connections}&theme=light&refresh=5s`}
                                    className="grafana-panel large"
                                    frameBorder="0"
                                    title="Database Connections"
                                />
                            </div>

                            {/* Gauges */}
                            <div className="panel-wrapper">
                                <h3>Trenutno Aktivne Konekcije</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.activeConn}&theme=light&refresh=5s`}
                                    className="grafana-panel medium"
                                    frameBorder="0"
                                    title="Active Connections"
                                />
                            </div>

                            <div className="panel-wrapper">
                                <h3>Trenutno Idle Konekcije</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.idleConn}&theme=light&refresh=5s`}
                                    className="grafana-panel medium"
                                    frameBorder="0"
                                    title="Idle Connections"
                                />
                            </div>
                        </div>

                        <div className="explanation-box">
                            <h4>Objašnjenje:</h4>
                            <ul>
                                <li><strong>Aktivne konekcije:</strong> Broj konekcija koje trenutno izvršavaju SQL upite (Total - Idle)</li>
                                <li><strong>Idle konekcije:</strong> Broj konekcija koje su otvorene i dostupne za korišćenje, ali trenutno ne izvršavaju upite</li>
                                <li><strong>Pool size:</strong> Maksimalno 20 konekcija (konfigurisano u application.properties)</li>
                                <li><strong>Minimum idle:</strong> Održava se minimum 5 idle konekcija za brzi odgovor</li>
                            </ul>
                        </div>
                    </div>
                )}

                {activeTab === 'cpu' && (
                    <div className="panels-section">
                        <h2>Zauzeće CPU</h2>

                        <div className="panels-grid">
                            {/* CPU Gauge */}
                            <div className="panel-wrapper">
                                <h3>Trenutno CPU Zauzeće</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.cpuGauge}&theme=light&refresh=5s`}
                                    className="grafana-panel medium"
                                    frameBorder="0"
                                    title="CPU Usage Gauge"
                                />
                            </div>

                            {/* Average CPU Graph */}
                            <div className="panel-wrapper full-width">
                                <h3>Prosečno CPU Zauzeće Tokom Vremena</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.cpuAvg}&theme=light&refresh=5s`}
                                    className="grafana-panel large"
                                    frameBorder="0"
                                    title="Average CPU Usage"
                                />
                            </div>
                        </div>

                        <div className="explanation-box">
                            <h4>Objašnjenje:</h4>
                            <ul>
                                <li><strong>System CPU Usage:</strong> Procenat iskorišćenosti CPU-a celokupnog sistema</li>
                                <li><strong>5 min avg:</strong> Prosečno CPU zauzeće u poslednjih 5 minuta</li>
                                <li><strong>15 min avg:</strong> Prosečno CPU zauzeće u poslednjih 15 minuta</li>
                                <li><strong>1h avg:</strong> Prosečno CPU zauzeće u poslednjem satu</li>
                                <li><strong>Threshold:</strong> Zeleno (&lt;50%), Žuto (50-80%), Crveno (&gt;80%)</li>
                            </ul>
                        </div>
                    </div>
                )}

                {activeTab === 'users' && (
                    <div className="panels-section">
                        <h2>Aktivni Korisnici</h2>

                        <div className="panels-grid">
                            {/* Current Users Gauge */}
                            <div className="panel-wrapper">
                                <h3>Trenutno Aktivnih Korisnika</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.currentUsers}&theme=light&refresh=5s&from=now-5m&to=now`}
                                    className="grafana-panel medium"
                                    frameBorder="0"
                                    title="Current Active Users"
                                />
                            </div>

                            {/* Active Users 24h Graph */}
                            <div className="panel-wrapper full-width">
                                <h3>Broj Aktivnih Korisnika (24h)</h3>
                                <iframe
                                    src={`${grafanaBaseUrl}/d-solo/${dashboardUid}/jutjubic-application-monitoring?orgId=1&panelId=${panels.activeUsers}&theme=light&refresh=5s`}
                                    className="grafana-panel large"
                                    frameBorder="0"
                                    title="Active Users 24h"
                                />
                            </div>
                        </div>

                        <div className="explanation-box">
                            <h4>Objašnjenje:</h4>
                            <ul>
                                <li><strong>Aktivan korisnik:</strong> Korisnik koji je napravio neki zahtev u poslednjih 5 minuta</li>
                                <li><strong>Praćenje:</strong> Svaki HTTP zahtev autentifikovanog korisnika se beleži</li>
                                <li><strong>Time range:</strong> Grafik prikazuje podatke za poslednjih 24 sata</li>
                                <li><strong>Implementacija:</strong> ActiveUserService + UserActivityInterceptor</li>
                                <li><strong>Storage:</strong> In-memory ConcurrentHashMap sa periodičnim čišćenjem</li>
                            </ul>
                        </div>
                    </div>
                )}

                {activeTab === 'full' && (
                    <div className="full-dashboard-section">
                        <h2>Kompletan Monitoring Dashboard</h2>
                        <div className="full-dashboard-wrapper">
                            <iframe
                                src={`${grafanaBaseUrl}/d/${dashboardUid}/jutjubic-application-monitoring?orgId=1&theme=light&refresh=5s&kiosk=tv`}
                                className="grafana-full-dashboard"
                                frameBorder="0"
                                title="Full Grafana Dashboard"
                            />
                        </div>
                        <div className="dashboard-actions">
                            <a 
                                href={`${grafanaBaseUrl}/d/${dashboardUid}/jutjubic-application-monitoring?orgId=1`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="btn-primary"
                            >
                                🔗 Otvori u Grafana (novi tab)
                            </a>
                        </div>
                    </div>
                )}
            </div>

            {/* Footer */}
            <footer className="monitoring-footer">
                <p>
                    Monitoring implementiran sa <strong>Prometheus</strong> i <strong>Grafana</strong>
                </p>
                <div className="footer-links">
                    <a href="http://localhost:8081/actuator/prometheus" target="_blank" rel="noopener noreferrer">
                        Prometheus Metrics
                    </a>
                    <span>•</span>
                    <a href="http://localhost:9090" target="_blank" rel="noopener noreferrer">
                        Prometheus UI
                    </a>
                    <span>•</span>
                    <a href="http://localhost:3001" target="_blank" rel="noopener noreferrer">
                        Grafana Dashboard
                    </a>
                </div>
            </footer>
        </div>
    );
};

export default MonitoringDashboard;
