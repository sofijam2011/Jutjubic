package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.UploadEvent;
import com.example.jutjubic.proto.UploadEventProto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UploadEventProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendJsonEvent(UploadEvent event) {
        System.out.println("📤 [JSON] Šaljem upload event: " + event);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_UPLOAD_JSON_QUEUE, event);
    }

    public void sendProtobufEvent(UploadEventProto.UploadEvent event) {
        System.out.println("📤 [PROTOBUF] Šaljem upload event: " + event.getVideoId());
        byte[] protoBytes = event.toByteArray();
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.VIDEO_UPLOAD_PROTOBUF_QUEUE,
            protoBytes
        );
    }
}
