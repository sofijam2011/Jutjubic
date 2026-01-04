package com.example.jutjubic.service;

import com.example.jutjubic.dto.CommentResponse;
import com.example.jutjubic.model.Comment;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentRateLimitService rateLimitService;

    /**
     * Keširana metoda za dobavljanje komentara sa paginacijom
     */
    @Cacheable(value = "comments", key = "#video.id + '-' + #page + '-' + #size")
    public Map<String, Object> getCommentsPaginated(Video video, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByVideoOrderByCreatedAtDesc(video, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentPage.getContent().stream()
                .map(this::toCommentResponse)
                .toList());
        response.put("currentPage", commentPage.getNumber());
        response.put("totalPages", commentPage.getTotalPages());
        response.put("totalComments", commentPage.getTotalElements());
        response.put("hasNext", commentPage.hasNext());
        response.put("hasPrevious", commentPage.hasPrevious());

        return response;
    }

    /**
     * Dodavanje komentara sa rate limitingom i kešom
     */
    @Transactional
    @CacheEvict(value = "comments", allEntries = true)
    public CommentResponse addComment(String text, Video video, User user) {
        // Provera rate limiting-a
        if (rateLimitService.isRateLimited(user.getId())) {
            throw new RuntimeException("Dostigli ste limit od 60 komentara po satu. Pokušajte ponovo kasnije.");
        }

        // Validacija teksta
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Komentar ne može biti prazan");
        }

        // Kreiranje komentara
        Comment comment = new Comment(text, video, user);
        Comment savedComment = commentRepository.save(comment);

        // Evidentiranje pokušaja za rate limiting
        rateLimitService.recordCommentAttempt(user.getId());

        return toCommentResponse(savedComment);
    }

    /**
     * Broj komentara za video (keširan)
     */
    @Cacheable(value = "commentCount", key = "#video.id")
    public long getCommentCount(Video video) {
        return commentRepository.countByVideo(video);
    }

    /**
     * Konverzija Comment -> CommentResponse
     */
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