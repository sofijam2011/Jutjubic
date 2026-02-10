package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.TranscodingMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Producer servis koji šalje transcoding poslove u RabbitMQ queue
 */
@Service
public class TranscodingProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Šalje poruku za transcoding u queue
     */
    public void sendTranscodingJob(TranscodingMessage message) {
        System.out.println("📤 Šaljem transcoding job u queue: " + message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_TRANSCODING_QUEUE, message);
        System.out.println("✅ Poruka uspešno poslata u queue");
    }
}
