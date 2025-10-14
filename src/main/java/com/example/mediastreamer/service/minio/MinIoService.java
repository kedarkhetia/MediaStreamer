package com.example.mediastreamer.service.minio;

import com.example.mediastreamer.model.PreSignedUploadUrlData;
import com.example.mediastreamer.model.VideoMetadata;
import com.google.gson.Gson;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.mediastreamer.utils.Constants.JSON_CONTENT_TYPE;
import static com.example.mediastreamer.utils.Constants.JSON_EXTENSION;
import static com.example.mediastreamer.utils.Constants.VIDEO_CONTENT_TYPE;

@Service
public class MinIoService {

    @Value("${minio.video.bucket}")
    private String videoBucket;

    @Value("${minio.video.metadata.bucket}")
    private String videoMetadataBucket;

    private final MinioClient minioClient;
    private final Gson gson;

    private static final int PRE_SIGNED_URL_TIMEOUT = 6; // hours

    public MinIoService(MinioClient minioClient) {
        this.gson = new Gson();
        this.minioClient = minioClient;
    }

    public boolean uploadVideo(MultipartFile file, String videoId) {
        try (InputStream inputStream = file.getInputStream()) {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(videoBucket).build())) {
                System.out.println("Bucket " + videoBucket + " does not exist!");
                return false;
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(videoBucket)
                    .contentType(VIDEO_CONTENT_TYPE)
                    .object(videoId)
                    .stream(inputStream, file.getSize(), -1)
                    .build());
            return true;
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading file to bucket: " + e);
            return false;
        }
    }

    public boolean uploadVideoMetadata(VideoMetadata metadata) {
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

    public VideoMetadata downloadVideoMetadata(String videoId) {
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

    public byte[] downloadVideo(String videoId, long offset, long length) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(videoBucket)
                .object(videoId)
                .offset(offset)
                .length(length)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            System.out.println("Video metadata for video with id: " + videoId + " not found!");
            return null;
        }
    }

    public InputStream getInputStreamForVideo(String videoId, long offset) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(videoBucket)
                .object(videoId)
                .offset(offset)
                .build())) {
            return stream;
        } catch (Exception e) {
            System.out.println("Failed to get InputStream for video with id: " + videoId);
            return null;
        }
    }

    public PreSignedUploadUrlData getPreSignedVideoUploadUrl(String videoId) {
        try {
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put("response-content-type", VIDEO_CONTENT_TYPE);

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
                    .contentType(VIDEO_CONTENT_TYPE)
                    .expiresIn(PRE_SIGNED_URL_TIMEOUT)
                    .videoId(videoId)
                    .build();
        } catch (Exception e) {
            System.out.println("Could not generate preSigned url for video upload");
            return null;
        }
    }
}
