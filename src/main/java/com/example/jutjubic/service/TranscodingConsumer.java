package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.TranscodingMessage;
import com.example.jutjubic.repository.VideoRepository;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TranscodingConsumer {

    @Autowired
    private FFmpegService ffmpegService;

    @Autowired
    private VideoRepository videoRepository;

    @RabbitListener(
            queues = RabbitMQConfig.VIDEO_TRANSCODING_QUEUE,
            concurrency = "2-4",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void processTranscodingJob(TranscodingMessage message, Channel channel, Message amqpMessage) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String consumerId = Thread.currentThread().getName();

        System.out.println("🎬 [" + consumerId + "] Primljena poruka za transcoding video ID: " + message.getVideoId());

        videoRepository.updateTranscodingStatus(message.getVideoId(), "IN_PROGRESS");

        try {
            ffmpegService.transcodeVideo(
                    message.getOriginalVideoPath(),
                    message.getOutputVideoPath(),
                    message.getParams()
            );

            videoRepository.updateTranscodingResult(
                    message.getVideoId(),
                    message.getOutputVideoPath(),
                    "COMPLETED"
            );

            System.out.println("✅ [" + consumerId + "] Transcoding završen za video ID: " + message.getVideoId()
                    + " -> " + message.getOutputVideoPath());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            System.err.println("❌ [" + consumerId + "] Greška pri transcodingu za video ID: " + message.getVideoId()
                    + " - " + e.getMessage());

            videoRepository.updateTranscodingStatus(message.getVideoId(), "FAILED");

            channel.basicNack(deliveryTag, false, false);
        }
    }
}
