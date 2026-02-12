package com.example.jutjubic.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class ActiveUserService {

    private static final int ACTIVE_USER_TIMEOUT_SECONDS = 300; // 5 minuta 
    
    private final Map<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger activeUserCount = new AtomicInteger(0);
    
    public ActiveUserService(MeterRegistry registry) {
        Gauge.builder("active_users_count", activeUserCount, AtomicInteger::get)
                .description("Broj trenutno aktivnih korisnika")
                .register(registry);
    }

    /**
     * Registruje aktivnost korisnika
     * @param username korisničko ime
     */
    public void registerUserActivity(String username) {
        if (username != null && !username.isEmpty()) {
            activeUsers.put(username, LocalDateTime.now());
            updateActiveUserCount();
        }
    }

    
    private void updateActiveUserCount() {
        LocalDateTime timeoutAgo = LocalDateTime.now().minusSeconds(ACTIVE_USER_TIMEOUT_SECONDS);
        activeUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(timeoutAgo));
        activeUserCount.set(activeUsers.size());
    }

    
    @Scheduled(fixedRate = 2000) // Svakih 2 sekunde - češća provera
    public void cleanupInactiveUsers() {
        updateActiveUserCount();
    }

    /**
     * Uklanja korisnika iz liste aktivnih (npr. pri logout-u)
     * @param username korisničko ime
     */
    public void removeUser(String username) {
        if (username != null && !username.isEmpty()) {
            activeUsers.remove(username);
            activeUserCount.set(activeUsers.size());
        }
    }

    
    public int getActiveUserCount() {
        updateActiveUserCount();
        return activeUserCount.get();
    }
}
