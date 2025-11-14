package com.example.mediastreamer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class VideoMetadata {
    private boolean isUploaded = false;
    @NonNull
    private String videoId;
    @NonNull
    private String fileName;
    @NonNull
    private String extension;
    @NonNull
    private String title;
    private double duration;
    @NonNull
    private List<String> tags;
    private Map<String, Integer> resolutions;
    private int totalChunks;
    private long size;
    private List<String> chunks;
}
