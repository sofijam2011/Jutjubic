package com.example.jutjubic.controller;

import com.example.jutjubic.dto.WatchPartyDTO;
import com.example.jutjubic.model.User;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.service.WatchPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API kontroler za Watch Party funkcionalnost
 */
@RestController
@RequestMapping("/api/watchparty")
public class WatchPartyController {

    @Autowired
    private WatchPartyService watchPartyService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Kreira novu Watch Party sobu
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestParam String name,
            @RequestParam(defaultValue = "true") Boolean isPublic,
            Authentication authentication) {

        try {
            User creator = getAuthenticatedUser(authentication);
            WatchPartyDTO room = watchPartyService.createRoom(name, creator, isPublic);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("room", room);
            response.put("message", "Soba kreirana: " + room.getRoomCode());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Vraća sve javne sobe
     */
    @GetMapping("/public")
    public ResponseEntity<List<WatchPartyDTO>> getPublicRooms() {
        return ResponseEntity.ok(watchPartyService.getPublicRooms());
    }

    /**
     * Vraća informacije o sobi po room code-u
     */
    @GetMapping("/room/{roomCode}")
    public ResponseEntity<?> getRoom(@PathVariable String roomCode) {
        try {
            WatchPartyDTO room = watchPartyService.getRoomByCode(roomCode);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Pridružuje korisnika sobi
     */
    @PostMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        try {
            User user = getAuthenticatedUser(authentication);
            WatchPartyDTO room = watchPartyService.joinRoom(roomCode, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("room", room);
            response.put("message", "Pridružio si se sobi: " + room.getName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Napušta sobu
     */
    @PostMapping("/leave/{roomCode}")
    public ResponseEntity<?> leaveRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        try {
            User user = getAuthenticatedUser(authentication);
            watchPartyService.leaveRoom(roomCode, user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Napustio si sobu"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Kreator pokreće video u sobi
     */
    @PostMapping("/room/{roomCode}/play")
    public ResponseEntity<?> playVideo(
            @PathVariable String roomCode,
            @RequestParam Long videoId,
            Authentication authentication) {

        try {
            User creator = getAuthenticatedUser(authentication);
            watchPartyService.playVideo(roomCode, videoId, creator);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video pokrenut za sve članove sobe"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Zatvara sobu
     */
    @PostMapping("/room/{roomCode}/close")
    public ResponseEntity<?> closeRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        try {
            User creator = getAuthenticatedUser(authentication);
            watchPartyService.closeRoom(roomCode, creator);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Soba zatvorena"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper metoda za dobijanje autentifikovanog korisnika
     */
    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Korisnik nije autentifikovan");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));
    }
}
