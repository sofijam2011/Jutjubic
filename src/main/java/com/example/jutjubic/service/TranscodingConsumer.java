package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.TranscodingMessage;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumer servis koji obrađuje transcoding poslove iz RabbitMQ queue-a
 * Spring Boot automatski kreira više listener thread-ova (minimum 2)
 */
@Service
public class TranscodingConsumer {

    @Autowired
    private FFmpegService ffmpegService;

    /**
     * Listener koji prima poruke iz queue-a
     * concurrency = "2-4" znači da će biti minimum 2, maksimum 4 concurrent consumera
     *
     * VAŽNO: Koristi MANUAL acknowledgment mode - poruka se ne briše dok ne pozovemo channel.basicAck()
     * Ovo sprečava da ista poruka bude obrađena više puta
     */
    @RabbitListener(
            queues = RabbitMQConfig.VIDEO_TRANSCODING_QUEUE,
            concurrency = "2-4",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void processTranscodingJob(TranscodingMessage message, Channel channel, Message amqpMessage) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String consumerId = Thread.currentThread().getName();

        System.out.println("🎬 [" + consumerId + "] Primljena poruka za transcoding: " + message);

        try {
            // Pozivamo FFmpeg servis za transcoding
            ffmpegService.transcodeVideo(
                    message.getOriginalVideoPath(),
                    message.getOutputVideoPath(),
                    message.getParams()
            );

            System.out.println("✅ [" + consumerId + "] Transcoding uspešno završen za video ID: " + message.getVideoId());

            // VAŽNO: Potvrđujemo da je poruka uspešno obrađena
            // Tek sada se poruka briše iz queue-a
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            System.err.println("❌ [" + consumerId + "] Greška prilikom transcodinga za video ID: " + message.getVideoId());
            e.printStackTrace();

            // Ako nije uspelo, vraćamo poruku u queue (ili šaljemo u DLQ)
            // false = ne requeue (šalje u Dead Letter Queue)
            // true = requeue (pokušaj ponovo)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
