package com.example.mediastreamer.mediaProcessor;

import com.example.mediastreamer.model.MinioAmqpEvent;
import com.example.mediastreamer.service.ffmpeg.FfmpegChunker;
import com.example.mediastreamer.service.rabbitmq.RabbitVideoChunkerConfig;
import com.google.gson.Gson;
import io.minio.messages.EventType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaChunker {

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Autowired
    private FfmpegChunker ffmpegChunker;

    private final Gson gson = new Gson();

    private static final String LISTENER_ID = "MediaChunker";

    public MediaChunker(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @RabbitListener(id = LISTENER_ID, queues = RabbitVideoChunkerConfig.QUEUE_NAME, autoStartup = "false")
    public void processVideoMessage(String message) {
        try {
            MinioAmqpEvent event = gson.fromJson(message, MinioAmqpEvent.class);
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                List<MinioAmqpEvent.Record> records = event.getRecords();
                for (MinioAmqpEvent.Record record : records) {
                    if (record.getEventName().equals(EventType.OBJECT_CREATED_PUT.toString())) {
                        MinioAmqpEvent.S3 s3 = record.getS3();
                        System.out.println(LISTENER_ID + ": Processing Video: " + s3.getS3Object().getKey());
                        ffmpegChunker.chunkVideoNatively(s3.getS3Object().getKey());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse message: " + e.getMessage());
        }
    }

    public void start() {
        MessageListenerContainer container = registry.getListenerContainer(LISTENER_ID);
        if (!container.isRunning()) {
            container.start();
            System.out.println(LISTENER_ID + ": Started Listening!");
        } else {
            System.out.println(LISTENER_ID + ": Already Running!");
        }
    }

    public void stop() {
        MessageListenerContainer container = registry.getListenerContainer(LISTENER_ID);
        if (container.isRunning()) {
            container.stop();
            System.out.println(LISTENER_ID + ": Stopped Listening!");
        } else {
            System.out.println(LISTENER_ID + ": Already Stopped!");
        }
    }
}


