package com.faceauth.faceauth.repository;

import com.faceauth.faceauth.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Long> {
    Optional<FaceEmbedding> findByUserId(Long userId);
}