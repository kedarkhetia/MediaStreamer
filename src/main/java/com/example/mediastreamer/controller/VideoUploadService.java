package com.example.mediastreamer.controller;

import com.example.mediastreamer.model.PreSignedUploadUrlData;
import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/upload")
public class VideoUploadService {

    @Autowired
    MinIoService minIoService;

    // Normal upload does not work for large files.
    @PostMapping("/normalVideo")
    public ResponseEntity<String> uploadNormalVideo(@RequestPart("file") MultipartFile file,
                                            @RequestPart("metadata") VideoMetadata metadata) {
        if (minIoService.uploadVideo(file, metadata.getVideoId())) {
            if (minIoService.uploadVideoMetadata(metadata))
                return ResponseEntity.ok("File upload successful");
        }
        return ResponseEntity.internalServerError().body("File upload failed!");
    }

    @PostMapping("/getPreSignedUploadUrl")
    public ResponseEntity<PreSignedUploadUrlData> getPreSignedUploadUrl(@RequestBody VideoMetadata metadata) {
        if (!minIoService.uploadVideoMetadata(metadata)) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(minIoService.getPreSignedVideoUploadUrl(metadata.getVideoId()));
    }

}
