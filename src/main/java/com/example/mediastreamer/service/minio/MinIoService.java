package com.example.mediastreamer.service.minio;

import com.example.mediastreamer.model.PreSignedDownloadUrlData;
import com.example.mediastreamer.model.PreSignedUploadUrlData;
import com.example.mediastreamer.model.VideoMetadata;
import com.google.gson.Gson;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.mediastreamer.utils.Constants.JSON_CONTENT_TYPE;
import static com.example.mediastreamer.utils.Constants.JSON_EXTENSION;
import static com.example.mediastreamer.utils.Constants.VIDEO_EXTENSION_KEY;

@Service
public class MinIoService {

    @Value("${minio.video.bucket}")
    private String videoBucket;

    @Value("${minio.video.metadata.bucket}")
    private String videoMetadataBucket;

    @Value("${minio.video.chunks.bucket}")
    private String videoChunksBucket;

    @Value("${minio.video.transcoded.chunks.bucket}")
    private String videoTranscodedChunksBucket;

    @Autowired
    private final MinioClient minioClient;

    private final Gson gson;

    private static final int PRE_SIGNED_URL_TIMEOUT = 6; // hours

    public MinIoService(MinioClient minioClient) {
        this.gson = new Gson();
        this.minioClient = minioClient;
    }

    public synchronized boolean uploadVideoMetadata(VideoMetadata metadata) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(videoMetadataBucket).build())) {
                System.out.println("Bucket " + videoMetadataBucket + " does not exist!");
                return false;
            }
            String metadataJson = gson.toJson(metadata);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(metadataJson.getBytes(StandardCharsets.UTF_8));
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(videoMetadataBucket)
                    .contentType(JSON_CONTENT_TYPE)
                    .object(metadata.getVideoId() + JSON_EXTENSION)
                    .stream(byteArrayInputStream, byteArrayInputStream.available(), -1)
                    .build());
            return true;
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading file metadata to bucket: " + e);
            return false;
        }
    }

    public void uploadChunkFile(String objectName, File localFile) throws Exception {
        minioClient.uploadObject(UploadObjectArgs
                .builder()
                .bucket(videoChunksBucket)
                .object(objectName)
                .filename(localFile.getAbsolutePath())
                .contentType("video/mp4")
                .build());
    }

    public synchronized VideoMetadata downloadVideoMetadata(String videoId) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(videoMetadataBucket)
                .object(videoId + JSON_EXTENSION)
                .build())) {
            String metadataJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return gson.fromJson(metadataJson, VideoMetadata.class);
        } catch (Exception e) {
            System.out.println("Video metadata for video with id: " + videoId + " not found!");
            return null;
        }
    }

    public synchronized PreSignedDownloadUrlData getPreSignedChunkDownloadUrl(String chunkId) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs
                    .builder()
                    .method(Method.GET)
                    .bucket(videoChunksBucket)
                    .object(chunkId)
                    .expiry(PRE_SIGNED_URL_TIMEOUT, TimeUnit.HOURS)
                    .build());
            return PreSignedDownloadUrlData.builder()
                    .chunkId(chunkId)
                    .preSignedUrl(url)
                    .build();
        } catch (Exception e) {
            System.out.println("Could not generate preSigned url for video download");
            return null;
        }
    }

    public synchronized PreSignedDownloadUrlData getPreSignedVideoDownloadUrl(String videoId) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs
                    .builder()
                    .method(Method.GET)
                    .bucket(videoBucket)
                    .object(videoId)
                    .expiry(PRE_SIGNED_URL_TIMEOUT, TimeUnit.HOURS)
                    .build());
            System.out.println("getPreSignedVideoDownloadUrl: " + url);
            return PreSignedDownloadUrlData.builder()
                    .chunkId(videoId)
                    .preSignedUrl(url)
                    .build();
        } catch (Exception e) {
            System.out.println("Could not generate preSigned url for video download");
            return null;
        }
    }

    public synchronized PreSignedUploadUrlData getPreSignedVideoUploadUrl(String videoId, String extension) {
        try {
            Map<String, String> reqParams = new HashMap<>();
            reqParams.put(VIDEO_EXTENSION_KEY, extension);
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs
                    .builder()
                    .method(Method.PUT)
                    .bucket(videoBucket)
                    .object(videoId)
                    .expiry(PRE_SIGNED_URL_TIMEOUT, TimeUnit.HOURS)
                    .extraQueryParams(reqParams)
                    .build());
            return PreSignedUploadUrlData.builder()
                    .preSignedUrl(url)
                    .expiresIn(PRE_SIGNED_URL_TIMEOUT)
                    .videoId(videoId)
                    .build();
        } catch (Exception e) {
            System.out.println("Could not generate preSigned url for video upload");
            return null;
        }
    }
}
