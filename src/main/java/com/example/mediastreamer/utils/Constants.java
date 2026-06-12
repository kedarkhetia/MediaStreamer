package com.example.mediastreamer.utils;

public class Constants {

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String MP4_CONTENT_TYPE = "video/mp4";
    public static final String JSON_EXTENSION = ".json";
    public static final String CHUNK_SECONDS = "4"; // 4 seconds
    public static final String CHUNK = "_chunk_%03d.mp4";
    public static final String ENCODED_CHUNK = "_encoded_";
    public static final String MP4_EXT = ".mp4";
    public static final String FFMPEG_CHUNKS_TMP_DIR_PREFIX = "ffmpeg_chunks_";
    public static final String RE_ENCODED_STRING = "libx264"; // Re-encode video track
    public static final String VISUAL_FIDELITY_PARITY = "23"; // Maintain visual fidelity parity
    public static final String VIDEO_EXTENSION_KEY = "VIDEO_FORMAT";
    public static final String VIDEO_TRANSCODER_420P_QUEUE_NAME = "video-transcoder-420p";
    public static final String VIDEO_TRANSCODER_720P_QUEUE_NAME = "video-transcoder-720p";
    public static final String VIDEO_TRANSCODER_1080P_QUEUE_NAME = "video-transcoder-1080p";
}
