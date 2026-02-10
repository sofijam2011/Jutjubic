package com.example.jutjubic.dto;

/**
 * Poruka koja se šalje preko WebSocket-a kada kreator promeni video
 */
public class VideoChangeMessage {

    private String roomCode;
    private Long videoId;
    private String videoTitle;
    private String creatorUsername;
    private String action; // "PLAY", "STOP", "CHANGE"

    public VideoChangeMessage() {}

    public VideoChangeMessage(String roomCode, Long videoId, String videoTitle, String creatorUsername, String action) {
        this.roomCode = roomCode;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.creatorUsername = creatorUsername;
        this.action = action;
    }

    // Getters and Setters
    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
