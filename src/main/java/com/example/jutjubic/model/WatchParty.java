package com.example.jutjubic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Watch Party soba za zajedničko gledanje videa
 */
@Entity
@Table(name = "watch_parties")
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomCode; // Jedinstveni kod za pristup sobi (npr: "ABCD1234")

    @Column(nullable = false)
    private String name; // Ime sobe

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator; // Kreator sobe

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_public")
    private Boolean isPublic = true; // Da li je soba javno dostupna

    @Column(name = "current_video_id")
    private Long currentVideoId; // Trenutni video koji se gleda

    @Column(name = "is_active")
    private Boolean isActive = true; // Da li je soba aktivna

    @OneToMany(mappedBy = "watchParty", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WatchPartyMember> members = new HashSet<>();

    public WatchParty() {
        this.roomCode = generateRoomCode();
    }

    public WatchParty(String name, User creator, Boolean isPublic) {
        this();
        this.name = name;
        this.creator = creator;
        this.isPublic = isPublic;
    }

    /**
     * Generiše jedinstveni kod sobe (8 karaktera)
     */
    private String generateRoomCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<WatchPartyMember> getMembers() {
        return members;
    }

    public void setMembers(Set<WatchPartyMember> members) {
        this.members = members;
    }
}
