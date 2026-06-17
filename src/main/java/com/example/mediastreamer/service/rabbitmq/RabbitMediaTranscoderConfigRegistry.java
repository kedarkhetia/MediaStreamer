package com.example.mediastreamer.service.rabbitmq;

import com.example.mediastreamer.mediaProcessor.MessageListenerTranscodeWorker;
import com.example.mediastreamer.service.ffmpeg.FfmpegTransformer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_PREFIX;

@Configuration
@Profile("RabbitMediaTranscoder")
public class RabbitMediaTranscoderConfigRegistry {

    private static final String EXCHANGE_NAME = "video-transcoder-exchange";
    private static final int ttl = 300000;

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerTranscoderContainer(
            ConnectionFactory connectionFactory,
            MessageListenerTranscodeWorker messageListenerTranscodeWorker,
            @Value("${worker.resolution}") int resolution
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setQueueNames(VIDEO_TRANSCODER_PREFIX + resolution);
        container.setMessageListener(messageListenerTranscodeWorker);
        container.setConnectionFactory(connectionFactory);
        container.setPrefetchCount(1);
        container.setAutoStartup(true);
        return container;
    }

    @Bean
    public MessageListenerTranscodeWorker messageListenerTranscodeWorker(
            FfmpegTransformer ffmpegTransformer,
            @Value("${worker.resolution}") int resolution,
            @Value("${worker.resolution-scale-command}") String resolutionCommand
    ) {
        String listenerId = "Worker-" + resolution + "p";
        return new MessageListenerTranscodeWorker(ffmpegTransformer, listenerId, resolution, resolutionCommand);
    }

    @Bean
    public FanoutExchange videoTranscoderExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue videoTranscoderQueue(@Value("${worker.resolution}") String resolution) {
        return QueueBuilder.durable(VIDEO_TRANSCODER_PREFIX + resolution)
                .ttl(ttl)
                .build();
    }

    @Bean
    public Binding videoTranscoderBinding(Queue videoTranscoderQueue, FanoutExchange videoTranscoderExchange) {
        return BindingBuilder.bind(videoTranscoderQueue).to(videoTranscoderExchange);
    }

}
