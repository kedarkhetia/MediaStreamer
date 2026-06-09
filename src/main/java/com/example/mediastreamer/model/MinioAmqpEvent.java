package com.example.mediastreamer.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class MinioAmqpEvent {

    @SerializedName("EventName")
    private String eventName;

    @SerializedName("Key")
    private String key;

    @SerializedName("Records")
    private List<Record> records;

    // --- Inner classes strictly for the exact data you want ---

    @Data
    public static class Record {
        private String eventName;
        private String eventTime;
        private S3 s3;
    }

    @Data
    public static class S3 {
        private Bucket bucket;
        @SerializedName("object")
        private S3Object s3Object;
    }

    @Data
    public static class Bucket {
        private String name;
        private String arn;
    }

    @Data
    public static class S3Object {
        private String key;
        private long size;
        private String contentType;
    }
}