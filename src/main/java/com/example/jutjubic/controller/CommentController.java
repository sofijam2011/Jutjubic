package com.example.jutjubic.controller;

import com.example.jutjubic.dto.CommentResponse;
import com.example.jutjubic.model.Comment;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.CommentRepository;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * JAVNO DOSTUPAN - Pregled svih komentara za video
     * Dostupno i autentifikovanim i neautentifikovanim korisnicima
     */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

        List<CommentResponse> comments = commentRepository.findByVideoOrderByCreatedAtDesc(video)
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(comments);
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
            if (text == null || text.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Komentar ne može biti prazan");
                return ResponseEntity.badRequest().body(error);
            }

            Comment comment = new Comment(text, video, user);
            Comment savedComment = commentRepository.save(comment);

            return ResponseEntity.ok(toCommentResponse(savedComment));
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

        long count = commentRepository.countByVideo(video);
        return ResponseEntity.ok(Map.of("count", count));
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getUser().getUsername(),
                comment.getUser().getId(),
                comment.getCreatedAt()
        );
    }
}