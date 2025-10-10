package com.example.mediastreamer.controller;

import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = "/stream")
public class VideoStreamerHttp {

    private static String BYTES = "bytes=";
    private static int BUFFER = 100000; // 100KB

    @Autowired
    MinIoService minIoService;

    @GetMapping("/video/{videoId}")
    public ResponseEntity<byte[]> streamVideo(@PathVariable("videoId") String videoId,
                                              @RequestHeader(value = "Range", required = false) String rangeHeader) {
        long rangeStart = 0;
        if (rangeHeader != null && rangeHeader.startsWith(BYTES)) {
            // skip the first "bytes="
            String[] ranges = rangeHeader.substring(6).split("-");
            rangeStart = Long.parseLong(ranges[0]);
        }
        VideoMetadata videoMetadata = minIoService.downloadVideoMetadata(videoId);
        if (videoMetadata == null) {
            String message = "failed to download video metadata!";
            return ResponseEntity.internalServerError().body(message.getBytes(StandardCharsets.UTF_8));
        }
        long length =  Math.min(BUFFER, videoMetadata.getSize() - rangeStart);
        byte[] videoBytes = minIoService.downloadVideo(videoId, rangeStart, length);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(videoBytes.length))
                // content-range are inclusive i.e. content-length 100 = content range 0 - 99
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + (rangeStart + length - 1) + "/" + videoMetadata.getSize())
                .body(videoBytes);
    }
}
