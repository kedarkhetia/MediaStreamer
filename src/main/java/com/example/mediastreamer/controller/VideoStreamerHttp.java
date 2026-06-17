package com.example.mediastreamer.controller;

import com.example.mediastreamer.model.PreSignedDownloadUrlData;
import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.database.H2DatabaseService;
import com.example.mediastreamer.service.minio.MinIoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/video")
public class VideoStreamerHttp {

    private static String BYTES = "bytes=";

    @Autowired
    private MinIoService minIoService;

    @Autowired
    private H2DatabaseService h2DatabaseService;

    @GetMapping("/{videoId}/metadata")
    public VideoMetadata getVideoMetadata(@PathVariable("videoId") String videoId) {
        return h2DatabaseService.getVideoMetadata(videoId);
    }

    @GetMapping("/getPresignedUrl/chunk/{chunkId}")
    public ResponseEntity<PreSignedDownloadUrlData> getPreSignedDownloadUrlForChunk(@PathVariable("chunkId") String chunkId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(minIoService.getPreSignedChunkDownloadUrl(chunkId));
    }

}
