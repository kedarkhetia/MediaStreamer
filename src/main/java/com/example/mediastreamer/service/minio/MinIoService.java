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
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.mediastreamer.utils.Constants.BUFFER;
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

    @Autowired
    private final MinioClient minioClient;

    private final Gson gson;

    private static final int PRE_SIGNED_URL_TIMEOUT = 6; // hours

    public MinIoService(MinioClient minioClient) {
        this.gson = new Gson();
        this.minioClient = minioClient;
    }

    public synchronized boolean uploadVideo(MultipartFile file, String videoId) {
        try (InputStream inputStream = file.getInputStream()) {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(videoBucket).build())) {
                System.out.println("Bucket " + videoBucket + " does not exist!");
                return false;
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(videoBucket)
                    .object(videoId)
                    .stream(inputStream, file.getSize(), -1)
                    .build());
            return true;
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading file to bucket: " + e);
            return false;
        }
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

    public synchronized void uploadVideoChunkUsingThreadStream(InputStream stream, String chunkId) {
        try {
            minioClient.putObject(PutObjectArgs
                    .builder()
                    .bucket(videoChunksBucket)
                    .object(chunkId)
                    .stream(stream, -1, BUFFER) // unknown chunk size
                    .build());
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading chunk with chunkId: " + chunkId
                    + " to bucket with bucketId: " + videoChunksBucket + ". Error: " + e);
            e.printStackTrace();
        }
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

    public synchronized byte[] downloadVideo(String videoId, long offset, long length) {
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

    public synchronized byte[] downloadVideoChunk(String chunkId) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(videoChunksBucket).object(chunkId).build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            System.out.println("chunk with id: " + chunkId + " not found!");
            return null;
        }
    }

    public synchronized InputStream getInputStreamForVideo(String videoId) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(videoBucket)
                    .object(videoId)
                    .build());
        } catch (Exception e) {
            System.out.println("Failed to get InputStream for video with id: " + videoId);
            return null;
        }
    }

    public synchronized PreSignedDownloadUrlData getPreSignedVideoDownloadUrl(String chunkId) {
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
