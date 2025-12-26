package com.example.mediastreamer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreSignedDownloadUrlData {
    private String preSignedUrl;
    private String chunkId;
}
