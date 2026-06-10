package com.example.mediastreamer.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitVideoTranscoderConfig {

    public static final String QUEUE_NAME = "video-transcoder";
    private static final String DL_QUEUE_NAME = "video-transcoder-dlq";
    private static final String EXCHANGE_NAME = "video-transcoder-exchange";
    private static final int ttl = 60000;

    @Bean
    public FanoutExchange videoTranscoderExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue videoTranscoderQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DL_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Queue videoTranscoderDeadLetterQueue() {
        return QueueBuilder.durable(DL_QUEUE_NAME).build();
    }

    @Bean
    public Binding videoTranscoderBinding(Queue videoTranscoderQueue, FanoutExchange videoTranscoderExchange) {
        return BindingBuilder.bind(videoTranscoderQueue).to(videoTranscoderExchange);
    }
}
