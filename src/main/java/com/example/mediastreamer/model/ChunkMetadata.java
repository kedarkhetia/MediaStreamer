package com.example.mediastreamer.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "chunk_metadata")
public class ChunkMetadata {
    @Id
    @Column(name = "chunk_id")
    private String chunkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private VideoMetadata videoMetadata;

    private Integer chunkIndex;

    @OneToMany(mappedBy = "chunkMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("targetResolution ASC")
    @Builder.Default
    private List<TranscodedChunkMetadata> transcodedChunks = new LinkedList<>();
}
