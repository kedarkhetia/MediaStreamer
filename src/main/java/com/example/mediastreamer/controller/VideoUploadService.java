package com.example.mediastreamer.controller;

import com.example.mediastreamer.model.PreSignedUploadUrlData;
import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.database.H2DatabaseService;
import com.example.mediastreamer.service.minio.MinIoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/upload")
public class VideoUploadService {

    @Autowired
    private MinIoService minIoService;

    @Autowired
    private H2DatabaseService h2DatabaseService;

    @PostMapping("/getPreSignedUploadUrl")
    public ResponseEntity<PreSignedUploadUrlData> getPreSignedUploadUrl(@RequestBody VideoMetadata metadata) {
        h2DatabaseService.saveVideoMetadata(metadata);
        return ResponseEntity.status(HttpStatus.OK)
                .body(minIoService.getPreSignedVideoUploadUrl(metadata.getVideoId(), metadata.getExtension()));
    }

}
