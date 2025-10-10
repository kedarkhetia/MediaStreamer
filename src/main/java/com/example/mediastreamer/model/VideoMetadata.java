package com.example.mediastreamer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Builder
@Data
public class VideoMetadata {
    @NonNull
    private String videoId;
    @NonNull
    private String fileName;
    @NonNull
    private String title;
    private long duration;
    @NonNull
    private List<String> tags;
    private long size;
}
