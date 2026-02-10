package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.UploadEvent;
import com.example.jutjubic.proto.UploadEventProto;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class UploadEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOAD_JSON_QUEUE)
    public void handleJsonUploadEvent(
            UploadEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {

        try {
            System.out.println("📥 [JSON] Upload event primljen:");
            System.out.println("   Video ID: " + event.getVideoId());
            System.out.println("   Naziv: " + event.getNaziv());
            System.out.println("   Veličina: " + formatBytes(event.getVelicina()));
            System.out.println("   Autor: " + event.getAutor());
            System.out.println("   Timestamp: " + event.getTimestamp());

            channel.basicAck(tag, false);
        } catch (Exception e) {
            System.err.println("❌ Greška pri obradi JSON upload event-a: " + e.getMessage());
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOAD_PROTOBUF_QUEUE)
    public void handleProtobufUploadEvent(
            byte[] messageBytes,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {

        try {
            UploadEventProto.UploadEvent event =
                UploadEventProto.UploadEvent.parseFrom(messageBytes);

            System.out.println("📥 [PROTOBUF] Upload event primljen:");
            System.out.println("   Video ID: " + event.getVideoId());
            System.out.println("   Naziv: " + event.getNaziv());
            System.out.println("   Veličina: " + formatBytes(event.getVelicina()));
            System.out.println("   Autor: " + event.getAutor());
            System.out.println("   Timestamp: " + event.getTimestamp());

            channel.basicAck(tag, false);
        } catch (Exception e) {
            System.err.println("❌ Greška pri obradi Protobuf upload event-a: " + e.getMessage());
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
