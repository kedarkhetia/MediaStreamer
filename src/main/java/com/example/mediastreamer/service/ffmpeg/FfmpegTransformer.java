package com.example.mediastreamer.service.ffmpeg;

import com.example.mediastreamer.model.ChunkMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static com.example.mediastreamer.utils.Constants.ENCODED_CHUNK;
import static com.example.mediastreamer.utils.Constants.FFMPEG_CHUNKS_TMP_DIR_PREFIX;
import static com.example.mediastreamer.utils.Constants.MP4_EXT;
import static com.example.mediastreamer.utils.Constants.RE_ENCODED_STRING;
import static com.example.mediastreamer.utils.Constants.VISUAL_FIDELITY_PARITY;
import static com.example.mediastreamer.utils.HelperMethods.cleanUpTempDirectory;

@Service
public class FfmpegTransformer {

    @Autowired
    private MinIoService minIoService;

    public void transformVideoNatively(String chunkIdOriginal, int resolution, String resolutionCommand) {
        File tempDir = null;

        try {
            // Get the stream URL from MinIO for FFmpeg input
            String chunkPreSignedUrl = minIoService.getPreSignedChunkDownloadUrl(chunkIdOriginal).getPreSignedUrl();

            String chunkId = chunkIdOriginal.split(MP4_EXT)[0];
            ChunkMetadata chunkMetadata = getChunkMetadata(chunkId);
            tempDir = Files.createTempDirectory(FFMPEG_CHUNKS_TMP_DIR_PREFIX + chunkId).toFile();

            // Transcode using native ffmpeg command
            List<String> command = new ArrayList<>();
            command.add("ffmpeg"); command.add("-y");
            command.add("-reconnect"); command.add("1");
            command.add("-reconnect_streamed"); command.add("1");
            command.add("-reconnect_delay_max"); command.add("5");
            command.add("-i"); command.add(chunkPreSignedUrl);
            command.add("-vf"); command.add(resolutionCommand);
            command.add("-c:v"); command.add(RE_ENCODED_STRING);
            command.add("-crf"); command.add(VISUAL_FIDELITY_PARITY);
            command.add("-preset"); command.add("medium");
            command.add("-c:a"); command.add("copy");
            command.add(tempDir.getAbsolutePath() + "/" + chunkId + ENCODED_CHUNK + resolution + MP4_EXT);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed while transcoding with exit code: " + exitCode);
            }

            File[] generatedFiles = tempDir.listFiles((dir, name) ->
                    name.startsWith(chunkId + ENCODED_CHUNK) && name.endsWith(MP4_EXT));

            if (generatedFiles != null) {
                for (File chunkFile : generatedFiles) {
                    String minioObjectName = chunkFile.getName();
                    minIoService.uploadTranscodedChunkFile(minioObjectName, chunkFile);
                    chunkMetadata.getTranscodedChunks().add(minioObjectName);
                }
            }

            minIoService.uploadVideoChunksMetadata(chunkMetadata);
        } catch (Exception e) {
            System.err.println("Transcoding or upload process failed!");
            throw new RuntimeException(e);
        } finally {
            if (tempDir != null && tempDir.exists()) {
                cleanUpTempDirectory(tempDir);
            }
        }
    }

    public ChunkMetadata getChunkMetadata(String chunkId) {
        ChunkMetadata chunkMetadata = minIoService.downloadVideoChunksMetadata(chunkId);
        if (chunkMetadata == null) {
            System.out.println("Creating new chunk metadata with chunkId: " + chunkId);
            chunkMetadata = ChunkMetadata
                    .builder()
                    .chunkId(chunkId)
                    .transcodedChunks(new TreeSet<>())
                    .build();
        }
        return chunkMetadata;
    }
}
