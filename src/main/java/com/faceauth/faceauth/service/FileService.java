package com.faceauth.faceauth.service;

import com.faceauth.faceauth.entity.FileEntity;
import com.faceauth.faceauth.repository.FileRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public FileEntity saveFile(FileEntity fileEntity) {

        fileEntity.setUploadTime(LocalDateTime.now());

        return fileRepository.save(fileEntity);
    }

    public List<FileEntity> getFilesByUserId(Long userId) {

        return fileRepository.findByUserId(userId);
    }

    public void deleteFile(Long id) {

        fileRepository.deleteById(id);
    }

}