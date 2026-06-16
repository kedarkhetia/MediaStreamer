package com.example.mediastreamer.mediaProcessor;

import com.example.mediastreamer.model.MinioAmqpEvent;
import com.example.mediastreamer.service.ffmpeg.FfmpegTransformer;
import com.google.gson.Gson;
import io.minio.messages.EventType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.example.mediastreamer.utils.Constants.X_AMZ_META_RESOLUTION_WIDTH_KEY;

public class MessageListenerTranscodeWorker implements MessageListener {

    private final Gson gson = new Gson();

    private FfmpegTransformer ffmpegTransformer;
    private String listenerId;
    private String resolutionCommand;
    private int resolution;

    public MessageListenerTranscodeWorker(FfmpegTransformer ffmpegTransformer, String listenerId,
                                          int resolution, String resolutionCommand) {
        this.ffmpegTransformer = ffmpegTransformer;
        this.listenerId = listenerId;
        this.resolution = resolution;
        this.resolutionCommand = resolutionCommand;
    }

    @Override
    public void onMessage(Message message) {
        try {
            MinioAmqpEvent event = gson.fromJson(new String(message.getBody(), StandardCharsets.UTF_8), MinioAmqpEvent.class);
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                List<MinioAmqpEvent.Record> records = event.getRecords();
                for (MinioAmqpEvent.Record record : records) {
                    if (record.getEventName().equals(EventType.OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD.toString())) {
                        MinioAmqpEvent.S3 s3 = record.getS3();
                        System.out.println(listenerId + ": Processing Video: " + s3.getS3Object().getKey());
                        int curResolution = Integer.parseInt(s3.getS3Object().getUserMetadata().get(X_AMZ_META_RESOLUTION_WIDTH_KEY));
                        System.out.println("Current Resolution: " + curResolution);

                        // We can skip transformation even when curResolution == resolution.
                        // However, we would need logic to copy the data with new id to
                        // video-transcoded-chunks bucket. For now, we are just transforming
                        // even when resolutions are same.
                        // TODO: Implement above.
                        if (curResolution < resolution) {
                            return;
                        }
                        ffmpegTransformer.transformVideoNatively(s3.getS3Object().getKey(), resolution, resolutionCommand);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse message: " + e.getMessage());
        }
    }
}
