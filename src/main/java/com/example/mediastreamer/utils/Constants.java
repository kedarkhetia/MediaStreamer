package com.example.mediastreamer.utils;

public class Constants {

    public static final String MP4_CONTENT_TYPE = "video/mp4";
    public static final String CHUNK_SECONDS = "4"; // 4 seconds
    public static final String CHUNK = "_chunk_%03d.mp4";
    public static final String ENCODED_CHUNK = "_encoded_";
    public static final String MP4_EXT = ".mp4";
    public static final String FFMPEG_CHUNKS_TMP_DIR_PREFIX = "ffmpeg_chunks_";
    public static final String RE_ENCODED_STRING = "libx264"; // Re-encode video track
    public static final String VISUAL_FIDELITY_PARITY = "23"; // Maintain visual fidelity parity
    public static final String VIDEO_EXTENSION_KEY = "VIDEO_FORMAT";
    public static final String RESOLUTION_METADATA_KEY_HEIGHT = "resolution-height";
    public static final String RESOLUTION_METADATA_KEY_WIDTH = "resolution-width";
    public static final String RESOLUTION_SPLIT_TOKEN = "x";
    public static final String X_AMZ_META_RESOLUTION_WIDTH_KEY = "X-Amz-Meta-Resolution-Width";
    public static final String VIDEO_TRANSCODER_PREFIX = "video-transcoder-";
}
