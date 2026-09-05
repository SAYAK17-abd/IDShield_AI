package com.project.document.entity;

import com.project.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Document entity storing metadata for uploaded identity files.
 * Protects file contents by storing them off-database in a dedicated storage path.
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_documents_owner", columnList = "owner_id"),
    @Index(name = "idx_documents_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String documentType; // e.g., PASSPORT, NATIONAL_ID, DRIVING_LICENSE

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, unique = true, length = 100)
    private String storedFilename; // Random UUID + safe extension

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false, length = 64)
    private String sha256Checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = DocumentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

