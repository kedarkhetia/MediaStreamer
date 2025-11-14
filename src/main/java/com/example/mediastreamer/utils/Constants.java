package com.example.mediastreamer.utils;

public class Constants {

    public static String JSON_CONTENT_TYPE = "application/json";
    public static String JSON_EXTENSION = ".json";
    public static final int CHUNK_SECONDS = 4; // 4 seconds
    public static final int THREAD_TIMEOUT = 100; // 1 second
    public static final int BUFFER = 5242880; // 5MiB
    public static final double MICROSECOND_TO_SECOND_MULTIPLIER = 1000000;
    public static final String CHUNK = "_chunk_";
    public static final String RESOLUTION = "_res_";
    public static final String DOT = ".";
    public static final int VIDEO_STREAM_BUFFER = 102400; // 1MB
    public static final String VIDEO_EXTENSION_KEY = "VIDEO_FORMAT";
    public static final String MOV_FLAGS = "movflags";
    public static final String FRAGMENTED_KEY_FRAMES = "frag_keyframe+empty_moov+default_base_moof";
    // public static final String FRAGMENTED_KEY_FRAMES = "empty_moov+default_base_moof";
    public static final String HIGH_RESOLUTION = "HIGH";
    public static final String LOW_RESOLUTION = "LOW";
}
