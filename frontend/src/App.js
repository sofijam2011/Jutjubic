import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [helloMessage, setHelloMessage] = useState('');
  const [error, setError] = useState(null);

  // Test connection on load
  useEffect(() => {
    testConnection();
    fetchMessages();
  }, []);

  // Test backend connection
  const testConnection = async () => {
    try {
      const response = await fetch('/api/hello');
      const data = await response.json();
      setHelloMessage(data.message);
      setError(null);
    } catch (err) {
      setError('Cannot connect to Spring Boot backend!');
      console.error('Connection error:', err);
    }
  };

  // Fetch all messages
  const fetchMessages = async () => {
    try {
      const response = await fetch('/api/messages');
      const data = await response.json();
      setMessages(data);
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  };

  // Add new message
  const addMessage = async () => {
    if (!newMessage.trim()) return;

    try {
      await fetch('/api/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: newMessage })
      });
      setNewMessage('');
      fetchMessages();
    } catch (err) {
      console.error('Error adding message:', err);
    }
  };

  return (
      <div className="App">
        <header className="App-header">
          <h1>🎬 Jutjubic - React + Spring Boot</h1>

          {/* Connection Status */}
          {error ? (
              <div style={{
                backgroundColor: '#ff4444',
                padding: '15px',
                borderRadius: '8px',
                marginBottom: '20px'
              }}>
                <strong>❌ {error}</strong>
                <br/>
                <small>Make sure Spring Boot is running on port 8081</small>
              </div>
          ) : (
              <div style={{
                backgroundColor: '#44ff44',
                color: '#000',
                padding: '15px',
                borderRadius: '8px',
                marginBottom: '20px'
              }}>
                <strong>✅ Connected!</strong> {helloMessage}
              </div>
          )}

          {/* Input Section */}
          <div style={{marginBottom: '30px'}}>
            <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder="Enter a message..."
                style={{
                  width: '400px',
                  padding: '12px',
                  fontSize: '16px',
                  borderRadius: '5px',
                  border: '2px solid #555'
                }}
                onKeyPress={(e) => e.key === 'Enter' && addMessage()}
            />
            <button
                onClick={addMessage}
                style={{
                  padding: '12px 30px',
                  marginLeft: '10px',
                  fontSize: '16px',
                  cursor: 'pointer',
                  borderRadius: '5px',
                  border: 'none',
                  backgroundColor: '#61dafb',
                  color: '#000',
                  fontWeight: 'bold'
                }}
            >
              Add
            </button>
          </div>

          {/* Messages List */}
          <div>
            <h2>Messages:</h2>
            {messages.length === 0 ? (
                <p>No messages yet. Add one above!</p>
            ) : (
                <ul style={{
                  listStyle: 'none',
                  padding: 0,
                  maxWidth: '600px'
                }}>
                  {messages.map((msg, index) => (
                      <li
                          key={index}
                          style={{
                            backgroundColor: '#282c34',
                            padding: '15px',
                            marginBottom: '10px',
                            borderRadius: '8px',
                            border: '2px solid #61dafb'
                          }}
                      >
                        {msg}
                      </li>
                  ))}
                </ul>
            )}
          </div>
        </header>
      </div>
  );
}

export default App;