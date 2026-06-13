package com.example.mediastreamer.service.ffmpeg;

import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import com.example.mediastreamer.utils.MediaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.example.mediastreamer.utils.Constants.CHUNK;
import static com.example.mediastreamer.utils.Constants.CHUNK_SECONDS;
import static com.example.mediastreamer.utils.Constants.FFMPEG_CHUNKS_TMP_DIR_PREFIX;
import static com.example.mediastreamer.utils.Constants.MP4_EXT;
import static com.example.mediastreamer.utils.Constants.RESOLUTION_METADATA_KEY_HEIGHT;
import static com.example.mediastreamer.utils.Constants.RESOLUTION_METADATA_KEY_WIDTH;
import static com.example.mediastreamer.utils.Constants.RESOLUTION_SPLIT_TOKEN;
import static com.example.mediastreamer.utils.HelperMethods.cleanUpTempDirectory;

/*
    The JVM-based video processing implementation using JavaCV/FFmpeg has been deprecated in favor of running native
    OS ffmpeg commands via ProcessBuilder. This shift addresses three critical production limitations:
    1. Application Stability: The old JavaCV library relies on heavy C-bindings that cause uncatchable
        Segmentation Faults (SIGSEGV) which crash the entire Spring Boot server whenever a video file
        has corrupted frames. Running a native OS process isolates these failures cleanly.
    2. Performance & Memory Management: Processing raw video frames directly inside Java puts massive
        pressure on the JVM heap, leading to severe Garbage Collection pauses. The native binary handles
        memory outside the JVM footprint entirely.
    3. Reliability & Simplicity: Instead of maintaining complex, custom multi-threaded code to manually
        detect I-frames, we now offload frame splitting to the native ffmpeg segment muxer, which natively
        guarantees perfectly synchronized keyframe cuts out-of-the-box.
 */

@Service
public class FfmpegChunker {

    @Autowired
    private MinIoService minIoService;


    public void chunkVideoNatively(String videoId) {
        File tempDir = null;
        try {
            VideoMetadata videoMetadata = minIoService.downloadVideoMetadata(videoId);
            tempDir = Files.createTempDirectory(FFMPEG_CHUNKS_TMP_DIR_PREFIX + videoId).toFile();

            // 1. Get the stream URL from MinIO for FFmpeg input
            String minioStreamUrl = minIoService.getPreSignedVideoDownloadUrl(videoId).getPreSignedUrl();
            String resolution = MediaUtils.getVideoResolution(minioStreamUrl);

            // 2. FFmpeg configuration (Streaming, zero-transcoding, exact naming)
            List<String> command = new ArrayList<>();
            command.add("ffmpeg"); command.add("-y");
            command.add("-reconnect"); command.add("1");
            command.add("-reconnect_streamed"); command.add("1");
            command.add("-reconnect_delay_max"); command.add("5");
            command.add("-i"); command.add(minioStreamUrl);
            command.add("-c"); command.add("copy");
            command.add("-f"); command.add("segment");
            command.add("-segment_time"); command.add(CHUNK_SECONDS);
            command.add("-reset_timestamps"); command.add("1");
            command.add(tempDir.getAbsolutePath() + "/" + videoId + CHUNK);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            // 3. Upload Output Files
            TreeSet<String> chunks = new TreeSet<>();
            File[] generatedFiles = tempDir.listFiles((dir, name) ->
                    name.startsWith(videoId) && name.endsWith(MP4_EXT));
            String[] resolutionHeightAndWidth = resolution.split(RESOLUTION_SPLIT_TOKEN);
            Map<String, String> metadata = Map.of(
                    RESOLUTION_METADATA_KEY_HEIGHT, resolutionHeightAndWidth[0],
                    RESOLUTION_METADATA_KEY_WIDTH, resolutionHeightAndWidth[1]
            );


            if (generatedFiles != null) {
                for (File chunkFile : generatedFiles) {
                    String minioObjectName = chunkFile.getName();
                    minIoService.uploadChunkFile(minioObjectName, metadata, chunkFile);
                    chunks.add(minioObjectName);
                }
            }

            // 4. Finalize Metadata
            videoMetadata.setTotalChunks(chunks.size());
            videoMetadata.setChunks(chunks.stream().toList());
            videoMetadata.setResolution(resolution);
            minIoService.uploadVideoMetadata(videoMetadata);

            System.out.println("Successfully streamed, chunked, and uploaded " + chunks.size() + " files.");

        } catch (Exception e) {
            System.err.println("Chunking or upload process failed!");
            e.printStackTrace();
        } finally {
            if (tempDir != null && tempDir.exists()) {
                cleanUpTempDirectory(tempDir);
            }
        }
    }


}