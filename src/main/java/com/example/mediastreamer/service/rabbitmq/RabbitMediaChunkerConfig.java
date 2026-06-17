package com.example.mediastreamer.service.rabbitmq;

import com.example.mediastreamer.mediaProcessor.MessageListenerMediaChunker;
import com.example.mediastreamer.service.ffmpeg.FfmpegChunker;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("RabbitMediaChunker")
public class RabbitMediaChunkerConfig {

    public static final String QUEUE_NAME = "video-chunker";
    private static final String DL_QUEUE_NAME = "video-chunker-dlq";
    private static final String EXCHANGE_NAME = "video-chunker-exchange";
    private static final String ROUTING_KEY = "video.upload";
    private static final int ttl = 300000;

    @Bean
    public MessageListenerMediaChunker messageListenerMediaChunker(
            FfmpegChunker chunker,
            @Value("${worker.chunker-id}") String chunkerId
    ) {
        return new MessageListenerMediaChunker(chunker, chunkerId);
    }

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerChunkerContainer(
            ConnectionFactory connectionFactory,
            MessageListenerMediaChunker mediaChunker
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setQueueNames(QUEUE_NAME);
        container.setMessageListener(mediaChunker);
        container.setConnectionFactory(connectionFactory);
        container.setPrefetchCount(1);
        container.setAutoStartup(true);
        return container;
    }

    @Bean
    public DirectExchange videoChunkerExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue videoChunkerQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DL_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Queue videoChunkerDeadLetterQueue() {
        return QueueBuilder.durable(DL_QUEUE_NAME).build();
    }

    @Bean
    public Binding videoChunkerBinding(Queue videoChunkerQueue, DirectExchange videoChunkerExchange) {
        return BindingBuilder.bind(videoChunkerQueue).to(videoChunkerExchange).with(ROUTING_KEY);
    }
}
