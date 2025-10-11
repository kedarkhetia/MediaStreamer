package com.example.mediastreamer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreSignedUploadUrlData {
    private String preSignedUrl;
    private String videoId;
    private int expiresIn;
    private String contentType;
}
