package com.example.jutjubic.listener;

import com.example.jutjubic.dto.VideoChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener za WebSocket događaje
 * Automatski šalje JOIN/LEAVE poruke kada se korisnici konektuju/diskonektuju
 */
@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // Mapiranje session ID -> (videoId, username)
    private final Map<String, SessionInfo> sessionRegistry = new ConcurrentHashMap<>();

    /**
     * Kada se korisnik poveže na WebSocket - čita username iz CONNECT zaglavlja
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Čitaj username iz STOMP CONNECT zaglavlja i sačuvaj u session attributes
        String username = headerAccessor.getFirstNativeHeader("username");
        if (username != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", username);
        }

        System.out.println("🔌 Nova WebSocket konekcija: " + sessionId + " (user: " + username + ")");
    }

    /**
     * Kada se korisnik pretplati na topic (npr. /topic/video/{videoId}/chat)
     * Automatski šalje JOIN poruku ostalim korisnicima
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        // Ekstraktuj username iz session attributes (ako postoji)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String username = sessionAttributes != null ?
                (String) sessionAttributes.get("username") : "Anonymous";

        // Proveri da li se pretplaćuje na video čet
        if (destination != null && destination.matches("/topic/video/\\d+/chat")) {
            // Ekstraktuj videoId iz destination
            String videoIdStr = destination.replaceAll(".*/video/(\\d+)/chat", "$1");
            try {
                Long videoId = Long.parseLong(videoIdStr);

                // Sačuvaj informacije o sesiji
                sessionRegistry.put(sessionId, new SessionInfo(videoId, username));

                // Pošalji JOIN poruku
                VideoChatMessage joinMessage = new VideoChatMessage();
                joinMessage.setUsername(username);
                joinMessage.setMessage(username + " se pridružio četu");
                joinMessage.setVideoId(videoId);
                joinMessage.setTimestamp(LocalDateTime.now());
                joinMessage.setType(VideoChatMessage.MessageType.JOIN);

                messagingTemplate.convertAndSend(destination, joinMessage);

                System.out.println("👋 " + username + " se pridružio četu za video " + videoId);

            } catch (NumberFormatException e) {
                System.err.println("❌ Greška pri parsiranju videoId: " + videoIdStr);
            }
        }
    }

    /**
     * Kada se korisnik diskontektuje
     * Automatski šalje LEAVE poruku ostalim korisnicima
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Dohvati informacije o sesiji
        SessionInfo sessionInfo = sessionRegistry.remove(sessionId);

        if (sessionInfo != null) {
            VideoChatMessage leaveMessage = new VideoChatMessage();
            leaveMessage.setUsername(sessionInfo.username);
            leaveMessage.setMessage(sessionInfo.username + " je napustio čet");
            leaveMessage.setVideoId(sessionInfo.videoId);
            leaveMessage.setTimestamp(LocalDateTime.now());
            leaveMessage.setType(VideoChatMessage.MessageType.LEAVE);

            String destination = "/topic/video/" + sessionInfo.videoId + "/chat";
            messagingTemplate.convertAndSend(destination, leaveMessage);

            System.out.println("👋 " + sessionInfo.username + " je napustio čet za video " + sessionInfo.videoId);
        }

        System.out.println("🔌 WebSocket diskonektovan: " + sessionId);
    }

    /**
     * Helper klasa za čuvanje informacija o sesiji
     */
    private static class SessionInfo {
        final Long videoId;
        final String username;

        SessionInfo(Long videoId, String username) {
            this.videoId = videoId;
            this.username = username;
        }
    }
}
