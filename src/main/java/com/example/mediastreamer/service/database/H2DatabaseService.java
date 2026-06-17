package com.example.mediastreamer.service.database;

import com.example.mediastreamer.model.ChunkMetadata;
import com.example.mediastreamer.model.TranscodedChunkMetadata;
import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.repositories.ChunkMetadataRepository;
import com.example.mediastreamer.repositories.VideoMatadataRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class H2DatabaseService {
    private final VideoMatadataRepository videoMatadataRepository;
    private final ChunkMetadataRepository chunkMetadataRepository;

    public H2DatabaseService(VideoMatadataRepository videoMatadataRepository, ChunkMetadataRepository chunkMetadataRepository) {
        this.videoMatadataRepository = videoMatadataRepository;
        this.chunkMetadataRepository = chunkMetadataRepository;
    }

    // Read APIs

    public VideoMetadata getVideoMetadata(String videoId) {
        return videoMatadataRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video with videoId:" + videoId + " not found in DB!"));
    }

    // Write APIs

    @Transactional
    public VideoMetadata saveVideoMetadata(VideoMetadata videoMetadata) {
        return videoMatadataRepository.save(videoMetadata);
    }

    @Transactional
    public ChunkMetadata saveChunkMetadata(ChunkMetadata chunkMetadata) {
        return chunkMetadataRepository.save(chunkMetadata);
    }

    @Transactional
    public void addTranscodedChunks(String chunkId,
                                    String transcodedChunkId,
                                    Integer resolution) {
        ChunkMetadata chunkMetadata = chunkMetadataRepository.findByIdForUpdate(chunkId)
                .orElseThrow(() -> new RuntimeException("Chunk with chunkId:" + chunkId + " not found in DB!"));

        TranscodedChunkMetadata transcodedChunkMetadata = TranscodedChunkMetadata.builder()
                .transcodedChunkId(transcodedChunkId)
                .chunkMetadata(chunkMetadata)
                .targetResolution(resolution)
                .build();
        chunkMetadata.getTranscodedChunks().add(transcodedChunkMetadata);
        chunkMetadataRepository.save(chunkMetadata);
    }

}
