package com.example.jutjubic.service;

import com.example.jutjubic.model.LoginAttempt;
import com.example.jutjubic.repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPT_WINDOW_MINUTES = 1;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    public void recordLoginAttempt(String ipAddress, boolean successful) {
        LoginAttempt attempt = new LoginAttempt(
                ipAddress,
                LocalDateTime.now(),
                successful
        );
        loginAttemptRepository.save(attempt);
    }

    public boolean isBlocked(String ipAddress) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now()
                .minusMinutes(ATTEMPT_WINDOW_MINUTES);

        List<LoginAttempt> recentAttempts = loginAttemptRepository
                .findRecentAttemptsByIp(ipAddress, oneMinuteAgo);

        // Broji samo neuspešne
        long failedAttempts = recentAttempts.stream()
                .filter(attempt -> !attempt.isSuccessful())
                .count();

        return failedAttempts >= MAX_ATTEMPTS;
    }

    @Scheduled(fixedRate = 3600000) // Čisti svaki sat
    @Transactional
    public void cleanOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        loginAttemptRepository.deleteByAttemptTimeBefore(cutoff);
    }
}