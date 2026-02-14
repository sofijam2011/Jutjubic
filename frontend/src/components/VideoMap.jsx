import React, { useState, useEffect, useCallback, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import API_BASE_URL from '../config';
import './VideoMap.css';

import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
});

L.Marker.prototype.options.icon = DefaultIcon;

const createClusterIcon = (count) => {
    const size = Math.min(20 + Math.log(count) * 10, 50);
    return L.divIcon({
        html: `<div class="cluster-marker">${count}</div>`,
        className: 'cluster-icon',
        iconSize: [size, size],
    });
};

const VideoMap = () => {
    const [videos, setVideos] = useState([]);
    const [timePeriod, setTimePeriod] = useState('ALL_TIME');
    const [loading, setLoading] = useState(false);
    const [currentZoom, setCurrentZoom] = useState(5);
    const [selectedVideo, setSelectedVideo] = useState(null);

    const mapRef = useRef(null);

    const fetchTilesForView = useCallback(async (bounds, zoom) => {
        setLoading(true);
        try {
            const tiles = calculateVisibleTiles(bounds, zoom);
            const allVideos = [];

            const tilePromises = tiles.map(tile =>
                fetch(`${API_BASE_URL}/api/map/tiles?zoom=${tile.zoom}&tileX=${tile.x}&tileY=${tile.y}&period=${timePeriod}`)
                    .then(res => res.json())
                    .catch(err => {
                        console.error(`Greška pri učitavanju tile-a ${tile.x},${tile.y}:`, err);
                        return [];
                    })
            );

            const results = await Promise.all(tilePromises);
            results.forEach(tileVideos => allVideos.push(...tileVideos));

            setVideos(allVideos);
        } catch (error) {
            console.error('Greška pri učitavanju video snimaka:', error);
        } finally {
            setLoading(false);
        }
    }, [timePeriod]);

    const calculateVisibleTiles = (bounds, zoom) => {
        const BASE_TILE_SIZE = 10.0;
        const tileSize = BASE_TILE_SIZE / Math.pow(2, zoom / 3.0);

        const minTileX = Math.floor((bounds.getWest() + 180) / tileSize);
        const maxTileX = Math.floor((bounds.getEast() + 180) / tileSize);
        const minTileY = Math.floor((bounds.getSouth() + 90) / tileSize);
        const maxTileY = Math.floor((bounds.getNorth() + 90) / tileSize);

        const tiles = [];
        for (let x = minTileX; x <= maxTileX; x++) {
            for (let y = minTileY; y <= maxTileY; y++) {
                tiles.push({ zoom, x, y });
            }
        }

        return tiles;
    };

    const MapEventHandler = () => {
        const map = useMapEvents({
            moveend: () => {
                const bounds = map.getBounds();
                const zoom = map.getZoom();
                const mappedZoom = mapZoomToTileZoom(zoom);
                setCurrentZoom(mappedZoom);
                fetchTilesForView(bounds, mappedZoom);
            },
        });

        useEffect(() => {
            mapRef.current = map;
        }, [map]);

        return null;
    };

    const mapZoomToTileZoom = (leafletZoom) => {
        if (leafletZoom <= 5) return 3;
        if (leafletZoom <= 9) return 6;
        return 9;
    };

    useEffect(() => {
        const initialBounds = L.latLngBounds(
            L.latLng(35, -10),
            L.latLng(71, 40)
        );
        fetchTilesForView(initialBounds, 3);
    }, [fetchTilesForView]);

    const handlePeriodChange = (period) => {
        setTimePeriod(period);
    };

    useEffect(() => {
        if (mapRef.current) {
            const bounds = mapRef.current.getBounds();
            const zoom = mapRef.current.getZoom();
            fetchTilesForView(bounds, mapZoomToTileZoom(zoom));
        }
    }, [timePeriod, fetchTilesForView]);

    return (
        <div className="video-map-container">
            <div className="map-header">
                <h1>Mapa Video Snimaka</h1>

                <div className="time-filter">
                    <button
                        className={timePeriod === 'ALL_TIME' ? 'active' : ''}
                        onClick={() => handlePeriodChange('ALL_TIME')}
                    >
                        Sve vreme
                    </button>
                    <button
                        className={timePeriod === 'LAST_30_DAYS' ? 'active' : ''}
                        onClick={() => handlePeriodChange('LAST_30_DAYS')}
                    >
                        Poslednjih 30 dana
                    </button>
                    <button
                        className={timePeriod === 'CURRENT_YEAR' ? 'active' : ''}
                        onClick={() => handlePeriodChange('CURRENT_YEAR')}
                    >
                        Tekuća godina
                    </button>
                </div>

                {loading && <div className="loading-indicator">Učitavanje...</div>}
                <div className="video-count">
                    Pronađeno: {videos.length} video snimaka
                </div>
            </div>

            <MapContainer
                center={[50.0, 15.0]}
                zoom={5}
                className="map-container"
            >
                <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                />

                <MapEventHandler />

                {videos.map((video) => {
                    const markerIcon = video.clusterSize > 1
                        ? createClusterIcon(video.clusterSize)
                        : DefaultIcon;

                    return (
                        <Marker
                            key={video.id}
                            position={[video.latitude, video.longitude]}
                            icon={markerIcon}
                            eventHandlers={{
                                click: () => setSelectedVideo(video),
                            }}
                        >
                            <Popup>
                                <div className="video-popup">

                                    <h3>{video.title}</h3>
                                    {video.clusterSize > 1 && (
                                        <p className="cluster-info">
                                             {video.clusterSize} video snimaka na ovoj lokaciji
                                        </p>
                                    )}
                                    <p className="video-stats">
                                        👁 {video.viewCount.toLocaleString()} pregleda
                                    </p>
                                    <p className="uploader">
                                        Autor: {video.uploaderName}
                                    </p>
                                    <p className="upload-date">
                                        {new Date(video.uploadDate).toLocaleDateString('sr-RS')}
                                    </p>
                                    <a
                                        href={`/video/${video.id}`}
                                        className="watch-btn"
                                        target="_blank"
                                        rel="noopener noreferrer"
                                    >
                                        Gledaj video
                                    </a>
                                </div>
                            </Popup>
                        </Marker>
                    );
                })}
            </MapContainer>

            {selectedVideo && (
                <div className="video-sidebar">
                    <button
                        className="close-sidebar"
                        onClick={() => setSelectedVideo(null)}
                    >
                        ✕
                    </button>
                    <img
                        src={`${API_BASE_URL}/api/videos/${selectedVideo.id}/thumbnail`}
                        alt={selectedVideo.title}
                        className="sidebar-thumbnail"
                        onError={(e) => {
                            e.target.onerror = null;
                            e.target.style.display = 'none';
                        }}
                    />
                    <h2>{selectedVideo.title}</h2>
                    <div className="sidebar-stats">
                        <span>👁 {selectedVideo.viewCount.toLocaleString()}</span>
                        <span> {selectedVideo.uploaderName}</span>
                    </div>
                    <p className="sidebar-date">
                        Postavljeno: {new Date(selectedVideo.uploadDate).toLocaleDateString('sr-RS')}
                    </p>
                    {selectedVideo.clusterSize > 1 && (
                        <div className="cluster-badge">
                             {selectedVideo.clusterSize} videa na lokaciji
                        </div>
                    )}
                    <a
                        href={`/video/${selectedVideo.id}`}
                        className="watch-btn-large"
                    >
                        Gledaj video
                    </a>
                </div>
            )}
        </div>
    );
};

export default VideoMap;
