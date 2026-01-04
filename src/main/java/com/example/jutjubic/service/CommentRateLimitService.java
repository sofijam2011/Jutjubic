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

    // IN-MEMORY KEŠIRANI BROJAČI - thread-safe
    private final ConcurrentHashMap<Long, AtomicInteger> userCommentCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> userWindowStartTimes = new ConcurrentHashMap<>();

    /**
     * Evidentiraj pokušaj komentarisanja
     * OPTIMIZOVANO: Prvo ažurira in-memory brojač, zatim asinkrono čuva u bazi
     */
    @CacheEvict(value = "commentRateLimit", key = "#userId")
    public synchronized void recordCommentAttempt(Long userId) {
        // Ažuriranje in-memory brojača (brzo)
        incrementUserCounter(userId);

        // Asinkrono čuvanje u bazi (sporo, ali ne blokira)
        saveAttemptAsync(userId);
    }

    /**
     * Proveri da li je korisnik prekoračio limit
     * OPTIMIZOVANO: Prvo proverava in-memory keš, tek onda bazu
     */
    @Cacheable(value = "commentRateLimit", key = "#userId")
    public boolean isRateLimited(Long userId) {
        // BRZA PROVERA: In-memory brojač (O(1) operacija)
        if (isLimitedByMemoryCounter(userId)) {
            return true;
        }

        // SPORA PROVERA: Baza (fallback ako nema u memoriji)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        List<CommentAttempt> recentAttempts = commentAttemptRepository
                .findRecentAttemptsByUser(userId, oneHourAgo);

        // Sinhronizuj memory counter sa bazom
        if (!recentAttempts.isEmpty()) {
            userCommentCounters.put(userId, new AtomicInteger(recentAttempts.size()));
            userWindowStartTimes.put(userId, LocalDateTime.now());
        }

        return recentAttempts.size() >= MAX_COMMENTS_PER_HOUR;
    }

    /**
     * Dobavi broj preostalih komentara u ovom satu
     */
    public int getRemainingComments(Long userId) {
        // Brza in-memory provera
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

    /**
     * IN-MEMORY COUNTER - thread-safe atomic operacija
     */
    private boolean isLimitedByMemoryCounter(Long userId) {
        if (!userCommentCounters.containsKey(userId)) {
            return false;
        }

        LocalDateTime windowStart = userWindowStartTimes.get(userId);
        if (windowStart == null) {
            return false;
        }

        // Provera da li je prozor istekao (prošlo više od 1 sata)
        if (windowStart.isBefore(LocalDateTime.now().minusHours(TIME_WINDOW_HOURS))) {
            // Reset brojača
            userCommentCounters.remove(userId);
            userWindowStartTimes.remove(userId);
            return false;
        }

        // Provera limita
        int currentCount = userCommentCounters.get(userId).get();
        return currentCount >= MAX_COMMENTS_PER_HOUR;
    }

    /**
     * Increment in-memory counter (atomic, thread-safe)
     */
    private void incrementUserCounter(Long userId) {
        userCommentCounters.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        userWindowStartTimes.putIfAbsent(userId, LocalDateTime.now());
    }

    /**
     * Asinkrono čuvanje u bazi (ne blokira glavnu nit)
     */
    @Transactional
    protected void saveAttemptAsync(Long userId) {
        CommentAttempt attempt = new CommentAttempt(userId, LocalDateTime.now());
        commentAttemptRepository.save(attempt);
    }

    /**
     * Čisti stare pokušaje svaki sat
     * DODATNO: Čisti i in-memory keš
     */
    @Scheduled(fixedRate = 3600000) // Svaki sat
    @Transactional
    public void cleanOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        commentAttemptRepository.deleteByCreatedAtBefore(cutoff);

        // Čišćenje in-memory keša
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        userWindowStartTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(oneHourAgo));
        userWindowStartTimes.keySet().forEach(userId -> {
            if (!userWindowStartTimes.containsKey(userId)) {
                userCommentCounters.remove(userId);
            }
        });
    }

    /**
     * Reset brojača (za testiranje)
     */
    public void resetCounters() {
        userCommentCounters.clear();
        userWindowStartTimes.clear();
    }
}