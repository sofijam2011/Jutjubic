package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoChangeMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket kontroler za Watch Party real-time komunikaciju
 */
@Controller
public class WatchPartyWebSocketController {

    /**
     * Prima poruke od klijenta i broadcast-uje ih svim pretplatnicima na topic
     *
     * Klijent šalje na: /app/watchparty/{roomCode}/video
     * Svi primaju na:   /topic/watchparty/{roomCode}
     */
    @MessageMapping("/watchparty/{roomCode}/video")
    @SendTo("/topic/watchparty/{roomCode}")
    public VideoChangeMessage broadcastVideoChange(
            @DestinationVariable String roomCode,
            VideoChangeMessage message) {

        System.out.println("📡 WebSocket poruka primljena za sobu: " + roomCode);
        System.out.println("   Video ID: " + message.getVideoId());
        System.out.println("   Akcija: " + message.getAction());

        // Prosleđivanje poruke svim pretplatnicima
        return message;
    }
}
