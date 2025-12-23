package com.example.jutjubic.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    private Long id;
    private String text;
    private String username;
    private Long userId;
    private LocalDateTime createdAt;

    // Constructor
    public CommentResponse() {}

    public CommentResponse(Long id, String text, String username, Long userId, LocalDateTime createdAt) {
        this.id = id;
        this.text = text;
        this.username = username;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}