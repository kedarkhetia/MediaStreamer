package com.example.mediastreamer.mediaProcessor;

import com.example.mediastreamer.service.ffmpeg.FfmpegTransformer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_1080P_QUEUE_NAME;
import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_420P_QUEUE_NAME;
import static com.example.mediastreamer.utils.Constants.VIDEO_TRANSCODER_720P_QUEUE_NAME;

@Configuration
public class MediaTranscoderRegistry {

    @Autowired
    private FfmpegTransformer ffmpegTransformer;

    // 420P Config
    public static final String MEDIA_TRANSCODER_420P = "MediaTranscoder420p";
    private static final String RESOLUTION_COMMAND_420P = "scale=-2:420";
    private static final int RESOLUTION_420P = 420;

    // 720P Config
    public static final String MEDIA_TRANSCODER_720P = "MediaTranscoder720p";
    private static final String RESOLUTION_COMMAND_720P = "scale=-2:720";
    private static final int RESOLUTION_720P = 720;

    // 1080P Config
    public static final String MEDIA_TRANSCODER_1080P = "MediaTranscoder1080p";
    private static final String RESOLUTION_COMMAND_1080P = "scale=-2:1080";
    private static final int RESOLUTION_1080P = 1080;

    @Bean
    public SimpleMessageListenerContainer registerMediaTranscoder420p(
            ConnectionFactory connectionFactory,
            FfmpegTransformer ffmpegTransformer
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(VIDEO_TRANSCODER_420P_QUEUE_NAME);
        container.setMessageListener(new MediaTranscoderMessageListener(
                ffmpegTransformer,
                MEDIA_TRANSCODER_420P,
                RESOLUTION_420P,
                RESOLUTION_COMMAND_420P)
        );
        container.setAutoStartup(true);
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer registerMediaTranscoder720p(
            ConnectionFactory connectionFactory,
            FfmpegTransformer ffmpegTransformer
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(VIDEO_TRANSCODER_720P_QUEUE_NAME);
        container.setMessageListener(new MediaTranscoderMessageListener(
                ffmpegTransformer,
                MEDIA_TRANSCODER_720P,
                RESOLUTION_720P,
                RESOLUTION_COMMAND_720P)
        );
        container.setAutoStartup(true);
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer registerMediaTranscoder1080p(
            ConnectionFactory connectionFactory,
            FfmpegTransformer ffmpegTransformer
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(VIDEO_TRANSCODER_1080P_QUEUE_NAME);
        container.setMessageListener(new MediaTranscoderMessageListener(
                ffmpegTransformer,
                MEDIA_TRANSCODER_1080P,
                RESOLUTION_1080P,
                RESOLUTION_COMMAND_1080P)
        );
        container.setAutoStartup(true);
        return container;
    }
}
