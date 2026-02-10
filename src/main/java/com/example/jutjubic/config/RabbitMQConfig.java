package com.example.jutjubic.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_TRANSCODING_QUEUE = "video.transcoding.queue";
    public static final String VIDEO_TRANSCODING_DLQ = "video.transcoding.dlq"; // Dead Letter Queue

    /**
     * Kreira queue za transcoding sa exactly-once delivery garantijom
     */
    @Bean
    public Queue videoTranscodingQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODING_QUEUE)
                .withArgument("x-dead-letter-exchange", "") // Default exchange
                .withArgument("x-dead-letter-routing-key", VIDEO_TRANSCODING_DLQ)
                .build();
    }

    /**
     * Dead Letter Queue - za poruke koje nisu uspešno obrađene
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODING_DLQ).build();
    }

    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate sa JSON converter-om
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Listener container factory sa acknowledgment mode MANUAL
     * Ovo omogućava exactly-once delivery - poruka se ne briše dok ne potvrdimo da je uspešno obrađena
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1); // Svaki consumer uzima po 1 poruku odjednom
        return factory;
    }
}
