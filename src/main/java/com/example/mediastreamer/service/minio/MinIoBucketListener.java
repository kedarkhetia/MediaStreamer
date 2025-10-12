package com.example.mediastreamer.service.minio;

import com.example.mediastreamer.model.VideoMetadata;
import io.minio.CloseableIterator;
import io.minio.ListenBucketNotificationArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Event;
import io.minio.messages.EventType;
import io.minio.messages.NotificationRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MinIoBucketListener {

    @Autowired
    private MinIoService minIoService;

    @Autowired
    private MinioClient minioClient;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Value("${minio.video.bucket}")
    private String videoBucket;

    @Value("${minio.video.metadata.bucket}")
    private String videoMetadataBucket;

    private static final String[] eventsToListen = {"s3:ObjectCreated:*", "s3:ObjectRemoved:*"};

    private CloseableIterator<Result<NotificationRecords>> bucketNotificationListener(String bucketName) {
        try {
            return minioClient.listenBucketNotification(ListenBucketNotificationArgs
                    .builder()
                    .bucket(bucketName)
                    .events(eventsToListen)
                    .build());
        } catch (Exception e) {
            System.out.println("Error occurred while listening to event: " +  e);
            return null;
        }
    }

    private void setVideoBucketNotificationListener() {
        try (CloseableIterator<Result<NotificationRecords>> videoBucketNotificationListener =
                     bucketNotificationListener(videoBucket)) {
            while (videoBucketNotificationListener != null && videoBucketNotificationListener.hasNext()) {
                Result<NotificationRecords> results = videoBucketNotificationListener.next();
                List<Event> events = results.get().events();
                for (Event event : events) {
                    if (event.eventType().equals(EventType.OBJECT_CREATED_PUT)) {
                        String videoId = event.objectName();
                        VideoMetadata videoMetadata = minIoService.downloadVideoMetadata(videoId);
                        if (videoMetadata == null) {
                            System.out.println("No video metadata found for uploaded video!");
                            return;
                        }
                        videoMetadata.setUploaded(true);
                        videoMetadata.setSize(event.objectSize());
                        minIoService.uploadVideoMetadata(videoMetadata);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to update video upload notification event!");
        }
    }

    @Bean
    public void enableMonitor() {
        executorService.submit(this::setVideoBucketNotificationListener);
    }


}
