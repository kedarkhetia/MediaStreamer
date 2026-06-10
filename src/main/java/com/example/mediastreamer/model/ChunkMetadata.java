package com.example.mediastreamer.model;

import lombok.Builder;
import lombok.Data;

import java.util.TreeSet;

@Builder
@Data
public class ChunkMetadata {
    private String chunkId;
    private TreeSet<String> transcodedChunks;

}
