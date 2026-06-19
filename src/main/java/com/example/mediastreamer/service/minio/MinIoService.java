package com.example.mediastreamer.service.minio;

import com.example.mediastreamer.model.PreSignedDownloadUrlData;
import com.example.mediastreamer.model.PreSignedUploadUrlData;
import com.google.gson.Gson;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.mediastreamer.utils.Constants.MP4_CONTENT_TYPE;
import static com.example.mediastreamer.utils.Constants.VIDEO_EXTENSION_KEY;

@Service
public class MinIoService {

    @Value("${minio.video.bucket}")
    private String videoBucket;

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

    public boolean uploadChunkFile(String objectName, Map<String, String> metadata, File localFile) {
        try {
            minioClient.uploadObject(UploadObjectArgs
                    .builder()
                    .bucket(videoChunksBucket)
                    .object(objectName)
                    .filename(localFile.getAbsolutePath())
                    .contentType(MP4_CONTENT_TYPE)
                    .userMetadata(metadata)
                    .build());
            return true;
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading chunk files to bucket: " + e);
            return false;
        }
    }

    public boolean uploadTranscodedChunkFile(String objectName, File localFile) {
        try {
            minioClient.uploadObject(UploadObjectArgs
                    .builder()
                    .bucket(videoTranscodedChunksBucket)
                    .object(objectName)
                    .filename(localFile.getAbsolutePath())
                    .contentType(MP4_CONTENT_TYPE)
                    .build());
            return true;
        } catch (Exception e) {
            System.out.println("Something went wrong while uploading transcoded chunk files to bucket: " + e);
            return false;
        }
    }

    public PreSignedDownloadUrlData getPreSignedChunkDownloadUrl(String chunkId) {
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

    public PreSignedDownloadUrlData getPreSignedVideoDownloadUrl(String videoId) {
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

    public PreSignedUploadUrlData getPreSignedVideoUploadUrl(String videoId, String extension) {
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
