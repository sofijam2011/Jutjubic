package com.example.jutjubic.dto;

import java.time.LocalDateTime;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private Long videoCount;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String username, String firstName, String lastName,
                               LocalDateTime createdAt, Long videoCount) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
        this.videoCount = videoCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVideoCount() { return videoCount; }
    public void setVideoCount(Long videoCount) { this.videoCount = videoCount; }
}
