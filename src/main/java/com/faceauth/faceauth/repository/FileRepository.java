package com.faceauth.faceauth.repository;

import com.faceauth.faceauth.entity.FileEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByUserId(Long userId);

}