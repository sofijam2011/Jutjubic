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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionRegistry = new ConcurrentHashMap<>();

    // videoId -> Set of unique usernames currently watching
    private final Map<Long, Set<String>> videoViewers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getFirstNativeHeader("username");
        if (username != null && accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put("username", username);
        }
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination == null) return;

        if (!destination.matches("/topic/video/\\d+/chat")) return;

        Map<String, Object> attrs = accessor.getSessionAttributes();
        String username = (attrs != null && attrs.get("username") != null)
                ? (String) attrs.get("username") : "Gost_" + sessionId;

        String videoIdStr = destination.replaceAll(".*/video/(\\d+)/chat", "$1");
        Long videoId;
        try {
            videoId = Long.parseLong(videoIdStr);
        } catch (NumberFormatException e) {
            return;
        }

        sessionRegistry.put(sessionId, new SessionInfo(videoId, username));

        Set<String> viewers = videoViewers.computeIfAbsent(videoId,
                k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));

        boolean isNewViewer = viewers.add(username);

        if (isNewViewer) {
            VideoChatMessage joinMsg = new VideoChatMessage();
            joinMsg.setUsername(username);
            joinMsg.setMessage(username + " se pridružio četu");
            joinMsg.setVideoId(videoId);
            joinMsg.setTimestamp(LocalDateTime.now());
            joinMsg.setType(VideoChatMessage.MessageType.JOIN);
            messagingTemplate.convertAndSend(destination, joinMsg);
        }

        broadcastViewerCount(videoId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        SessionInfo info = sessionRegistry.remove(sessionId);
        if (info == null) return;

        // Provjeri da li isti username ima još aktivnih sesija za ovaj video
        boolean stillHasSession = sessionRegistry.values().stream()
                .anyMatch(s -> info.videoId.equals(s.videoId) && info.username.equals(s.username));

        if (!stillHasSession) {
            Set<String> viewers = videoViewers.get(info.videoId);
            if (viewers != null) {
                viewers.remove(info.username);
                if (viewers.isEmpty()) videoViewers.remove(info.videoId);
            }

            VideoChatMessage leaveMsg = new VideoChatMessage();
            leaveMsg.setUsername(info.username);
            leaveMsg.setMessage(info.username + " je napustio čet");
            leaveMsg.setVideoId(info.videoId);
            leaveMsg.setTimestamp(LocalDateTime.now());
            leaveMsg.setType(VideoChatMessage.MessageType.LEAVE);

            messagingTemplate.convertAndSend("/topic/video/" + info.videoId + "/chat", leaveMsg);
            broadcastViewerCount(info.videoId);
        }
    }

    public int getViewerCount(Long videoId) {
        return videoViewers.getOrDefault(videoId, Collections.emptySet()).size();
    }

    private void broadcastViewerCount(Long videoId) {
        Set<String> viewers = videoViewers.getOrDefault(videoId, Collections.emptySet());
        System.out.println("[WS] broadcastViewerCount videoId=" + videoId + " count=" + viewers.size());

        VideoChatMessage countMsg = new VideoChatMessage();
        countMsg.setVideoId(videoId);
        countMsg.setType(VideoChatMessage.MessageType.VIEWER_COUNT);
        countMsg.setViewerCount(viewers.size());
        countMsg.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/video/" + videoId + "/viewers", countMsg);
    }

    private static class SessionInfo {
        final Long videoId;
        final String username;

        SessionInfo(Long videoId, String username) {
            this.videoId = videoId;
            this.username = username;
        }
    }
}
