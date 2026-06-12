package com.example.mediastreamer.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_1080P_QUEUE_NAME;
import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_420P_QUEUE_NAME;
import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_720P_QUEUE_NAME;

@Configuration
public class RabbitMediaTranscoderConfigRegistry {

    private static final String EXCHANGE_NAME = "video-transcoder-exchange";
    private static final int ttl = 60000;

    @Bean
    public FanoutExchange videoTranscoderExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue videoTranscoder420pQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODER_420P_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Queue videoTranscoder720pQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODER_720P_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Queue videoTranscoder1080pQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODER_1080P_QUEUE_NAME)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Binding videoTranscoder420pBinding(Queue videoTranscoder420pQueue, FanoutExchange videoTranscoderExchange) {
        return BindingBuilder.bind(videoTranscoder420pQueue).to(videoTranscoderExchange);
    }

    @Bean
    public Binding videoTranscoder720pBinding(Queue videoTranscoder720pQueue, FanoutExchange videoTranscoderExchange) {
        return BindingBuilder.bind(videoTranscoder720pQueue).to(videoTranscoderExchange);
    }

    @Bean
    public Binding videoTranscoder1080pBinding(Queue videoTranscoder1080pQueue, FanoutExchange videoTranscoderExchange) {
        return BindingBuilder.bind(videoTranscoder1080pQueue).to(videoTranscoderExchange);
    }
}
