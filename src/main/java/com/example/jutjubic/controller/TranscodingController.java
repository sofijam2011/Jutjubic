package com.example.jutjubic.controller;

import com.example.jutjubic.service.FFmpegService;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/transcoding")
public class TranscodingController {

    @Autowired
    private FFmpegService ffmpegService;

    @Autowired(required = false)
    private RabbitAdmin rabbitAdmin;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();

        boolean ffmpegInstalled = ffmpegService.isFFmpegInstalled();
        health.put("ffmpeg", Map.of(
                "installed", ffmpegInstalled,
                "status", ffmpegInstalled ? "OK" : "NOT_INSTALLED"
        ));

        boolean rabbitmqConnected = false;
        try {
            if (rabbitAdmin != null) {
                Properties queueProperties = rabbitAdmin.getQueueProperties("video.transcoding.queue");
                rabbitmqConnected = queueProperties != null;

                if (rabbitmqConnected) {
                    health.put("rabbitmq", Map.of(
                            "connected", true,
                            "status", "OK",
                            "queue", "video.transcoding.queue"
                    ));
                }
            }
        } catch (Exception e) {
            health.put("rabbitmq", Map.of(
                    "connected", false,
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }

        if (!rabbitmqConnected && rabbitAdmin == null) {
            health.put("rabbitmq", Map.of(
                    "connected", false,
                    "status", "NOT_CONFIGURED"
            ));
        }

        boolean healthy = ffmpegInstalled && rabbitmqConnected;
        health.put("status", healthy ? "HEALTHY" : "UNHEALTHY");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("description", "Video Transcoding System with RabbitMQ");
        info.put("consumers", "Minimum 2 concurrent workers");
        info.put("queue", "video.transcoding.queue");
        info.put("acknowledgment", "MANUAL (exactly-once delivery)");
        info.put("ffmpeg_presets", Map.of(
                "720p", "1280x720, 2000k bitrate, libx264, aac",
                "1080p", "1920x1080, 4000k bitrate, libx264, aac"
        ));
        info.put("rabbitmq_ui", "http://localhost:15672");
        info.put("setup_guide", "See TRANSCODING_SETUP.md");

        return ResponseEntity.ok(info);
    }
}
