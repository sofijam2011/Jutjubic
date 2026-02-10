package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoChangeMessage;
import com.example.jutjubic.dto.WatchPartyDTO;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.model.WatchParty;
import com.example.jutjubic.model.WatchPartyMember;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.repository.WatchPartyMemberRepository;
import com.example.jutjubic.repository.WatchPartyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servis za upravljanje Watch Party sobama
 */
@Service
public class WatchPartyService {

    @Autowired
    private WatchPartyRepository watchPartyRepository;

    @Autowired
    private WatchPartyMemberRepository memberRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Kreira novu Watch Party sobu
     */
    @Transactional
    public WatchPartyDTO createRoom(String name, User creator, Boolean isPublic) {
        WatchParty watchParty = new WatchParty(name, creator, isPublic);
        watchParty = watchPartyRepository.save(watchParty);

        // Automatski dodaj kreatora kao člana
        WatchPartyMember creatorMember = new WatchPartyMember(watchParty, creator);
        memberRepository.save(creatorMember);

        return toDTO(watchParty);
    }

    /**
     * Vraća sve javne aktivne sobe
     */
    public List<WatchPartyDTO> getPublicRooms() {
        return watchPartyRepository.findByIsPublicTrueAndIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Vraća sobu po room code-u
     */
    public WatchPartyDTO getRoomByCode(String roomCode) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Soba ne postoji"));
        return toDTO(watchParty);
    }

    /**
     * Pridružuje korisnika sobi
     */
    @Transactional
    public WatchPartyDTO joinRoom(String roomCode, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Soba ne postoji"));

        if (!watchParty.getIsActive()) {
            throw new IllegalStateException("Soba nije aktivna");
        }

        // Proveri da li je korisnik već član
        if (!memberRepository.existsByWatchPartyIdAndUserId(watchParty.getId(), user.getId())) {
            WatchPartyMember member = new WatchPartyMember(watchParty, user);
            memberRepository.save(member);
        }

        return toDTO(watchParty);
    }

    /**
     * Napušta sobu
     */
    @Transactional
    public void leaveRoom(String roomCode, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Soba ne postoji"));

        memberRepository.deleteByWatchPartyIdAndUserId(watchParty.getId(), user.getId());

        // Ako je kreator napustio sobu, deaktiviraj je
        if (watchParty.getCreator().getId().equals(user.getId())) {
            watchParty.setIsActive(false);
            watchPartyRepository.save(watchParty);
        }
    }

    /**
     * Kreator pokreće video - broadcast svim članovima
     */
    @Transactional
    public void playVideo(String roomCode, Long videoId, User creator) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Soba ne postoji"));

        // Proveri da li je korisnik kreator
        if (!watchParty.getCreator().getId().equals(creator.getId())) {
            throw new IllegalStateException("Samo kreator može da pokreće videe");
        }

        // Ažuriraj trenutni video
        watchParty.setCurrentVideoId(videoId);
        watchPartyRepository.save(watchParty);

        // Preuzmi video info
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video ne postoji"));

        // Broadcast poruku svim članovima sobe preko WebSocket-a
        VideoChangeMessage message = new VideoChangeMessage(
                roomCode,
                videoId,
                video.getTitle(),
                creator.getUsername(),
                "PLAY"
        );

        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);

        System.out.println("🎬 Video播放: " + video.getTitle() + " u sobi " + roomCode);
    }

    /**
     * Zatvara sobu
     */
    @Transactional
    public void closeRoom(String roomCode, User creator) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Soba ne postoji"));

        if (!watchParty.getCreator().getId().equals(creator.getId())) {
            throw new IllegalStateException("Samo kreator može da zatvori sobu");
        }

        watchParty.setIsActive(false);
        watchPartyRepository.save(watchParty);

        // Obavesti sve članove da je soba zatvorena
        VideoChangeMessage message = new VideoChangeMessage(
                roomCode,
                null,
                null,
                creator.getUsername(),
                "STOP"
        );

        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);
    }

    /**
     * Konvertuje entitet u DTO
     */
    private WatchPartyDTO toDTO(WatchParty watchParty) {
        WatchPartyDTO dto = new WatchPartyDTO();
        dto.setId(watchParty.getId());
        dto.setRoomCode(watchParty.getRoomCode());
        dto.setName(watchParty.getName());
        dto.setCreatorUsername(watchParty.getCreator().getUsername());
        dto.setCreatorId(watchParty.getCreator().getId());
        dto.setCreatedAt(watchParty.getCreatedAt());
        dto.setIsPublic(watchParty.getIsPublic());
        dto.setCurrentVideoId(watchParty.getCurrentVideoId());
        dto.setIsActive(watchParty.getIsActive());

        // Dodaj broj članova
        long memberCount = memberRepository.countByWatchPartyId(watchParty.getId());
        dto.setMemberCount((int) memberCount);

        // Dodaj članove
        List<WatchPartyDTO.MemberDTO> members = memberRepository.findByWatchPartyId(watchParty.getId())
                .stream()
                .map(m -> new WatchPartyDTO.MemberDTO(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getJoinedAt(),
                        m.getIsOnline()
                ))
                .collect(Collectors.toList());
        dto.setMembers(members);

        // Dodaj trenutni video title ako postoji
        if (watchParty.getCurrentVideoId() != null) {
            videoRepository.findById(watchParty.getCurrentVideoId())
                    .ifPresent(video -> dto.setCurrentVideoTitle(video.getTitle()));
        }

        return dto;
    }
}
