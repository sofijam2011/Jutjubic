import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import API_BASE_URL from '../config';
import './LiveChat.css';

const LiveChat = ({ videoId }) => {
    const [messages, setMessages] = useState([]);
    const [inputText, setInputText] = useState('');
    const [connected, setConnected] = useState(false);
    const clientRef = useRef(null);
    const messagesEndRef = useRef(null);

    const userStr = localStorage.getItem('user');
    const username = userStr ? JSON.parse(userStr).username : 'Gost_' + Math.floor(Math.random() * 1000);

    useEffect(() => {
        const wsUrl = `${API_BASE_URL}/ws`;

        const client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            connectHeaders: { username },
            reconnectDelay: 5000,
            onConnect: () => {
                setConnected(true);
                client.subscribe(`/topic/video/${videoId}/chat`, (msg) => {
                    const data = JSON.parse(msg.body);
                    setMessages(prev => [...prev, data]);
                });
            },
            onDisconnect: () => setConnected(false),
        });

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
        };
    }, [videoId]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const sendMessage = () => {
        if (!inputText.trim() || !clientRef.current?.connected) return;

        clientRef.current.publish({
            destination: `/app/video/${videoId}/chat`,
            body: JSON.stringify({
                username,
                message: inputText.trim(),
                videoId: parseInt(videoId),
                type: 'CHAT',
            }),
        });

        setInputText('');
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') sendMessage();
    };

    return (
        <div className="live-chat">
            <div className="live-chat-header">
                <span>💬 Live čet</span>
                <span className={`chat-status ${connected ? 'online' : 'offline'}`}>
                    {connected ? '● Uživo' : '○ Konekcija...'}
                </span>
            </div>

            <div className="live-chat-messages">
                {messages.map((msg, i) => (
                    <div
                        key={i}
                        className={`chat-msg ${msg.type === 'JOIN' || msg.type === 'LEAVE' ? 'system' : ''}`}
                    >
                        {msg.type === 'JOIN' || msg.type === 'LEAVE' ? (
                            <span className="system-text">{msg.message}</span>
                        ) : (
                            <>
                                <span className="chat-username">{msg.username}</span>
                                <span className="chat-text">{msg.message}</span>
                            </>
                        )}
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            <div className="live-chat-input">
                <input
                    type="text"
                    placeholder={connected ? 'Napišite poruku...' : 'Povezivanje...'}
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={!connected}
                    maxLength={300}
                />
                <button onClick={sendMessage} disabled={!connected || !inputText.trim()}>
                    Pošalji
                </button>
            </div>
        </div>
    );
};

export default LiveChat;
