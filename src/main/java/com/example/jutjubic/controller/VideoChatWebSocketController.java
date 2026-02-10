package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;

@Controller
public class VideoChatWebSocketController {

    @MessageMapping("/video/{videoId}/chat")
    @SendTo("/topic/video/{videoId}/chat")
    public VideoChatMessage handleChatMessage(
            @DestinationVariable Long videoId,
            VideoChatMessage message) {

        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        message.setVideoId(videoId);

        System.out.println("📨 Chat poruka za video " + videoId +
                           " od korisnika: " + message.getUsername());

        return message;
    }
}
