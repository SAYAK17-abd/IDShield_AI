package com.project.document.service;

import com.project.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates uploaded documents to defend against malicious files,
 * disguised executables, extension spoofing, and path traversal.
 */
@Slf4j
@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    // Magic Bytes Signatures
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or missing");
        }

        // 1. File Size Verification
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException("File exceeds maximum allowed size of 10MB");
        }

        // 2. Sanitize and Verify Filename & Extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidFileException("Filename is required");
        }

        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new InvalidFileException("Filename contains illegal path traversal characters");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidFileException("File type not supported. Allowed formats: PDF, JPEG, PNG");
        }

        // 3. Content-Type Header Verification
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Invalid file MIME type: " + contentType);
        }

        // 4. Deep Inspection: Magic Bytes / File Signature Verification
        validateMagicBytes(file, extension);
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 3) {
                throw new InvalidFileException("Malformed or unreadable file");
            }

            switch (extension.toLowerCase()) {
                case "pdf":
                    if (!startsWith(header, PDF_MAGIC)) {
                        throw new InvalidFileException("File content does not match PDF signature (magic bytes check failed)");
                    }
                    break;
                case "jpg":
                case "jpeg":
                    if (!startsWith(header, JPEG_MAGIC)) {
                        throw new InvalidFileException("File content does not match JPEG signature (magic bytes check failed)");
                    }
                    break;
                case "png":
                    if (!startsWith(header, PNG_MAGIC)) {
                        throw new InvalidFileException("File content does not match PNG signature (magic bytes check failed)");
                    }
                    break;
                default:
                    throw new InvalidFileException("Unsupported file extension: " + extension);
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read file for signature verification");
        }
    }

    private boolean startsWith(byte[] source, byte[] match) {
        if (source.length < match.length) return false;
        for (int i = 0; i < match.length; i++) {
            if (source[i] != match[i]) return false;
        }
        return true;
    }

    public String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastIndex + 1);
    }
}

