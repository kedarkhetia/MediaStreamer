package com.example.mediastreamer.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "video_metadata")
public class VideoMetadata {

    @Id
    @Column(name = "video_id")
    private String videoId;

    private String fileName;
    private String extension;
    private String title;
    private String resolution;
    private Integer totalChunks;
    private Long size;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @OneToMany(mappedBy = "videoMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chunkIndex ASC")
    @Builder.Default
    private List<ChunkMetadata> chunks = new LinkedList<>();

    public enum ProcessingStatus {
        UPLOADING, CHUNKING, TRANSCODING, READY, FAILED
    }
}
