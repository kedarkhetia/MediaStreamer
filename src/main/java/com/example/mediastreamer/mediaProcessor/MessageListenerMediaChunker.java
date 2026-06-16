package com.example.mediastreamer.mediaProcessor;

import com.example.mediastreamer.model.MinioAmqpEvent;
import com.example.mediastreamer.service.ffmpeg.FfmpegChunker;
import com.google.gson.Gson;
import io.minio.messages.EventType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

import java.util.List;

public class MessageListenerMediaChunker  implements MessageListener {

    private FfmpegChunker ffmpegChunker;
    private String listenerId;

    private final Gson gson = new Gson();

    public MessageListenerMediaChunker(FfmpegChunker ffmpegChunker, String listenerId) {
        this.ffmpegChunker = ffmpegChunker;
        this.listenerId = listenerId;
    }

    @Override
    public void onMessage(Message message) {
        try {
            MinioAmqpEvent event = gson.fromJson(new String(message.getBody()), MinioAmqpEvent.class);
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                List<MinioAmqpEvent.Record> records = event.getRecords();
                for (MinioAmqpEvent.Record record : records) {
                    if (record.getEventName().equals(EventType.OBJECT_CREATED_PUT.toString())) {
                        MinioAmqpEvent.S3 s3 = record.getS3();
                        System.out.println(listenerId + ": Processing Video: " + s3.getS3Object().getKey());
                        ffmpegChunker.chunkVideoNatively(s3.getS3Object().getKey());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse message: " + e.getMessage());
        }
    }
}


