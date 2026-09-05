package com.project.document.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file, String storedFilename) throws IOException;
    Resource loadFileAsResource(String storagePath);
    void deleteFile(String storagePath);
    String calculateSha256(MultipartFile file) throws IOException;
}

