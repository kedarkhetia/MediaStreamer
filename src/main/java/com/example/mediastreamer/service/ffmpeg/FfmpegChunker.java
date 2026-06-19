package com.example.mediastreamer.service.ffmpeg;

import com.example.mediastreamer.model.ChunkMetadata;
import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.database.H2DatabaseService;
import com.example.mediastreamer.service.minio.MinIoService;
import com.example.mediastreamer.utils.MediaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private H2DatabaseService h2DatabaseService;


    public void chunkVideoNatively(String videoId) {
        File tempDir = null;
        Process process = null;

        try {
            VideoMetadata videoMetadata = h2DatabaseService.getVideoMetadata(videoId);
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
            command.add("-segment_time");
            command.add(CHUNK_SECONDS);
            command.add("-reset_timestamps");
            command.add("1");
            command.add(tempDir.getAbsolutePath() + "/" + videoId + CHUNK);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            // 3. Upload Output Files
            File[] generatedFiles = tempDir.listFiles((dir, name) ->
                    name.startsWith(videoId) && name.endsWith(MP4_EXT));
            String[] resolutionHeightAndWidth = resolution.split(RESOLUTION_SPLIT_TOKEN);
            Map<String, String> metadata = Map.of(
                    RESOLUTION_METADATA_KEY_HEIGHT, resolutionHeightAndWidth[0],
                    RESOLUTION_METADATA_KEY_WIDTH, resolutionHeightAndWidth[1]
            );


            if (generatedFiles != null) {
                Arrays.sort(generatedFiles);
                List<ChunkMetadata> chunksMetadata = new LinkedList<>();
                for (int i=0; i<generatedFiles.length; i++) {
                    File chunkFile = generatedFiles[i];
                    minIoService.uploadChunkFile(chunkFile.getName(), metadata, chunkFile);
                    ChunkMetadata chunkMetadata = ChunkMetadata.builder()
                            .chunkId(chunkFile.getName())
                            .chunkIndex(i)
                            .videoMetadata(videoMetadata)
                            .build();
                    h2DatabaseService.saveChunkMetadata(chunkMetadata);
                    chunksMetadata.add(chunkMetadata);
                }
                // 4. Finalize Metadata
                videoMetadata.setTotalChunks(chunksMetadata.size());
                videoMetadata.setChunks(chunksMetadata);
                videoMetadata.setResolution(resolution);
                h2DatabaseService.saveVideoMetadata(videoMetadata);
                System.out.println("Successfully streamed, chunked, and uploaded " + chunksMetadata.size() + " files.");
            }
        } catch (Exception e) {
            System.err.println("Chunking or upload process failed!");
            if (process != null) { process.destroyForcibly(); }
            e.printStackTrace();
        } finally {
            if (tempDir != null && tempDir.exists()) {
                cleanUpTempDirectory(tempDir);
            }
        }
    }
}