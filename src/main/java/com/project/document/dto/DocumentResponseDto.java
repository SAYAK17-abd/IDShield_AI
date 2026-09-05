package com.project.document.dto;

import com.project.document.entity.Document;
import com.project.document.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Safe document metadata DTO.
 * Excludes internal server storage paths.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {
    private Long id;
    private Long ownerId;
    private String ownerEmail;
    private String documentType;
    private String originalFilename;
    private Long fileSizeBytes;
    private String mimeType;
    private String sha256Checksum;
    private DocumentStatus status;
    private Instant createdAt;

    public static DocumentResponseDto fromEntity(Document doc) {
        return DocumentResponseDto.builder()
                .id(doc.getId())
                .ownerId(doc.getOwner().getId())
                .ownerEmail(doc.getOwner().getEmail())
                .documentType(doc.getDocumentType())
                .originalFilename(doc.getOriginalFilename())
                .fileSizeBytes(doc.getFileSizeBytes())
                .mimeType(doc.getMimeType())
                .sha256Checksum(doc.getSha256Checksum())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}

