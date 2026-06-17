package com.example.mediastreamer.repositories;

import com.example.mediastreamer.model.ChunkMetadata;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChunkMetadataRepository extends JpaRepository<ChunkMetadata, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChunkMetadata c WHERE c.chunkId = :chunkId")
    Optional<ChunkMetadata> findByIdForUpdate(@Param("chunkId") String chunkId);
}
