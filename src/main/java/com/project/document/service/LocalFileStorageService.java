package com.project.document.service;

import com.project.exception.ApiException;
import com.project.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Local File Storage Service implementation.
 * Ensures storage isolation outside the webroot and prevents directory traversal.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${application.storage.upload-dir:./uploads/documents}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Secure document storage initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new ApiException("Could not create document storage directory", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String storedFilename) throws IOException {
        Path targetLocation = this.fileStorageLocation.resolve(storedFilename).normalize();

        // Path Traversal Defense: Ensure the target file is strictly inside the designated directory
        if (!targetLocation.startsWith(this.fileStorageLocation)) {
            throw new ApiException("Cannot store file outside current storage directory", HttpStatus.BAD_REQUEST);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        return targetLocation.toString();
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new ApiException("Unauthorized path access attempt", HttpStatus.FORBIDDEN);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or not readable: " + filePath.getFileName());
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path malformed");
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();
            if (filePath.startsWith(this.fileStorageLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file at path: {}", storagePath, e);
        }
    }

    @Override
    public String calculateSha256(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}

