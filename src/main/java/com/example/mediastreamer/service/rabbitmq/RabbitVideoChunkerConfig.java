package com.example.mediastreamer.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitVideoChunkerConfig {

    public static final String QUEUE_NAME = "video-chunker";
    private static final String DL_QUEUE_NAME = "video-chunker-dlq";
    private static final String EXCHANGE_NAME = "video-chunker-exchange";
    private static final String ROUTING_KEY = "video.upload";
    private static final int ttl = 60000;

    @Bean
    public DirectExchange videoExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue videoQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DL_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DL_QUEUE_NAME).build();
    }

    @Bean
    public Binding videoBinding(Queue videoQueue, DirectExchange videoExchange) {
        return BindingBuilder.bind(videoQueue).to(videoExchange).with(ROUTING_KEY);
    }
}
