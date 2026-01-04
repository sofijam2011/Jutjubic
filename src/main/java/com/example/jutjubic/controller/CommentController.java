package com.example.jutjubic.controller;

import com.example.jutjubic.dto.CommentResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.service.CommentRateLimitService;
import com.example.jutjubic.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRateLimitService rateLimitService;

    /**
     * JAVNO DOSTUPAN - Pregled komentara sa paginacijom
     * Dostupno i autentifikovanim i neautentifikovanim korisnicima
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

        Map<String, Object> response = commentService.getCommentsPaginated(video, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * ZAHTEVA AUTENTIFIKACIJU - Dodavanje novog komentara
     * Neautentifikovani korisnici će dobiti 401 Unauthorized
     */
    @PostMapping
    public ResponseEntity<?> addComment(
            @PathVariable Long videoId,
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        // Provera autentifikacije
        if (authentication == null || authentication.getName() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Morate biti prijavljeni da biste ostavili komentar");
            return ResponseEntity.status(401).body(error);
        }

        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

            String text = request.get("text");

            CommentResponse comment = commentService.addComment(text, video, user);
            return ResponseEntity.ok(comment);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * JAVNO DOSTUPAN - Broj komentara za video
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCommentCount(@PathVariable Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

        long count = commentService.getCommentCount(video);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * ZAHTEVA AUTENTIFIKACIJU - Provera preostalog broja komentara
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Integer>> getRateLimitStatus(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

        int remaining = rateLimitService.getRemainingComments(user.getId());
        return ResponseEntity.ok(Map.of("remainingComments", remaining));
    }
}