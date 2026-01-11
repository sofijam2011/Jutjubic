package com.example.jutjubic.service;

import com.example.jutjubic.model.CommentAttempt;
import com.example.jutjubic.repository.CommentAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CommentRateLimitService {

    private static final int MAX_COMMENTS_PER_HOUR = 60;
    private static final int TIME_WINDOW_HOURS = 1;

    @Autowired
    private CommentAttemptRepository commentAttemptRepository;

    private final ConcurrentHashMap<Long, AtomicInteger> userCommentCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> userWindowStartTimes = new ConcurrentHashMap<>();

    @CacheEvict(value = "commentRateLimit", key = "#userId")
    public synchronized void recordCommentAttempt(Long userId) {
        incrementUserCounter(userId);
        saveAttemptAsync(userId);
    }

    @Cacheable(value = "commentRateLimit", key = "#userId")
    public boolean isRateLimited(Long userId) {
        if (isLimitedByMemoryCounter(userId)) {
            return true;
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        List<CommentAttempt> recentAttempts = commentAttemptRepository
                .findRecentAttemptsByUser(userId, oneHourAgo);

        if (!recentAttempts.isEmpty()) {
            userCommentCounters.put(userId, new AtomicInteger(recentAttempts.size()));
            userWindowStartTimes.put(userId, LocalDateTime.now());
        }

        return recentAttempts.size() >= MAX_COMMENTS_PER_HOUR;
    }

    public int getRemainingComments(Long userId) {
        if (userCommentCounters.containsKey(userId)) {
            LocalDateTime windowStart = userWindowStartTimes.get(userId);
            if (windowStart != null && windowStart.isAfter(LocalDateTime.now().minusHours(TIME_WINDOW_HOURS))) {
                int used = userCommentCounters.get(userId).get();
                return Math.max(0, MAX_COMMENTS_PER_HOUR - used);
            }
        }

        // Fallback na bazu
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        List<CommentAttempt> recentAttempts = commentAttemptRepository
                .findRecentAttemptsByUser(userId, oneHourAgo);

        int used = recentAttempts.size();
        return Math.max(0, MAX_COMMENTS_PER_HOUR - used);
    }

    private boolean isLimitedByMemoryCounter(Long userId) {
        if (!userCommentCounters.containsKey(userId)) {
            return false;
        }

        LocalDateTime windowStart = userWindowStartTimes.get(userId);
        if (windowStart == null) {
            return false;
        }

        if (windowStart.isBefore(LocalDateTime.now().minusHours(TIME_WINDOW_HOURS))) {
            userCommentCounters.remove(userId);
            userWindowStartTimes.remove(userId);
            return false;
        }

        int currentCount = userCommentCounters.get(userId).get();
        return currentCount >= MAX_COMMENTS_PER_HOUR;
    }

    private void incrementUserCounter(Long userId) {
        userCommentCounters.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        userWindowStartTimes.putIfAbsent(userId, LocalDateTime.now());
    }

    @Transactional
    protected void saveAttemptAsync(Long userId) {
        CommentAttempt attempt = new CommentAttempt(userId, LocalDateTime.now());
        commentAttemptRepository.save(attempt);
    }

    @Scheduled(fixedRate = 3600000) // Svaki sat
    @Transactional
    public void cleanOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        commentAttemptRepository.deleteByCreatedAtBefore(cutoff);

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        userWindowStartTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(oneHourAgo));
        userWindowStartTimes.keySet().forEach(userId -> {
            if (!userWindowStartTimes.containsKey(userId)) {
                userCommentCounters.remove(userId);
            }
        });
    }

    public void resetCounters() {
        userCommentCounters.clear();
        userWindowStartTimes.clear();
    }
}