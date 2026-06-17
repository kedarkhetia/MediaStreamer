package com.example.mediastreamer.repositories;

import com.example.mediastreamer.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoMatadataRepository extends JpaRepository<VideoMetadata, String> {
}
