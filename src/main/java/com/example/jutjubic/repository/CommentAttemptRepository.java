package com.example.jutjubic.repository;

import com.example.jutjubic.model.CommentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentAttemptRepository extends JpaRepository<CommentAttempt, Long> {

    @Query("SELECT c FROM CommentAttempt c WHERE c.userId = :userId " +
            "AND c.createdAt > :since ORDER BY c.createdAt DESC")
    List<CommentAttempt> findRecentAttemptsByUser(Long userId, LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
}
