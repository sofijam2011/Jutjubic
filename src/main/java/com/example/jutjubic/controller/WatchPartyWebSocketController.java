package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoChangeMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyWebSocketController {

    @MessageMapping("/watchparty/{roomCode}/video")
    @SendTo("/topic/watchparty/{roomCode}")
    public VideoChangeMessage broadcastVideoChange(
            @DestinationVariable String roomCode,
            VideoChangeMessage message) {

        System.out.println("📡 WebSocket poruka primljena za sobu: " + roomCode);
        System.out.println("   Video ID: " + message.getVideoId());
        System.out.println("   Akcija: " + message.getAction());

        return message;
    }
}
