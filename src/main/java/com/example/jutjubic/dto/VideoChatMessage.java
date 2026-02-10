package com.example.jutjubic.dto;

import java.time.LocalDateTime;

public class VideoChatMessage {
    private String username;
    private String message;
    private Long videoId;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        JOIN,    // Korisnik se pridružio
        CHAT,    // Obična poruka
        LEAVE    // Korisnik napustio
    }

    public VideoChatMessage() {}

    public VideoChatMessage(String username, String message, Long videoId, LocalDateTime timestamp, MessageType type) {
        this.username = username;
        this.message = message;
        this.videoId = videoId;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
}
