package com.example.jutjubic.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO za Watch Party sobu
 */
public class WatchPartyDTO {

    private Long id;
    private String roomCode;
    private String name;
    private String creatorUsername;
    private Long creatorId;
    private LocalDateTime createdAt;
    private Boolean isPublic;
    private Long currentVideoId;
    private String currentVideoTitle;
    private Boolean isActive;
    private Integer memberCount;
    private List<MemberDTO> members;

    public WatchPartyDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Long getCurrentVideoId() {
        return currentVideoId;
    }

    public void setCurrentVideoId(Long currentVideoId) {
        this.currentVideoId = currentVideoId;
    }

    public String getCurrentVideoTitle() {
        return currentVideoTitle;
    }

    public void setCurrentVideoTitle(String currentVideoTitle) {
        this.currentVideoTitle = currentVideoTitle;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public List<MemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDTO> members) {
        this.members = members;
    }

    /**
     * DTO za člana sobe
     */
    public static class MemberDTO {
        private Long userId;
        private String username;
        private LocalDateTime joinedAt;
        private Boolean isOnline;

        public MemberDTO() {}

        public MemberDTO(Long userId, String username, LocalDateTime joinedAt, Boolean isOnline) {
            this.userId = userId;
            this.username = username;
            this.joinedAt = joinedAt;
            this.isOnline = isOnline;
        }

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }

        public Boolean getIsOnline() {
            return isOnline;
        }

        public void setIsOnline(Boolean isOnline) {
            this.isOnline = isOnline;
        }
    }
}
