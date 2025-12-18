package com.example.jutjubic.repository;

import com.example.jutjubic.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT l FROM LoginAttempt l WHERE l.ipAddress = :ipAddress " +
            "AND l.attemptTime > :since ORDER BY l.attemptTime DESC")
    List<LoginAttempt> findRecentAttemptsByIp(String ipAddress, LocalDateTime since);

    void deleteByAttemptTimeBefore(LocalDateTime cutoffTime);
}