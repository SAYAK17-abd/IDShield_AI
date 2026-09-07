package com.project.verification.entity;

import com.project.document.entity.Document;
import com.project.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * VerificationResult JPA entity.
 * Stores comprehensive AI screening results, transparent risk assessment breakdown,
 * detected inconsistencies, and human investigator review actions.
 */
@Entity
@Table(name = "verification_results", indexes = {
    @Index(name = "idx_verification_document", columnList = "document_id", unique = true),
    @Index(name = "idx_verification_risk_level", columnList = "riskLevel"),
    @Index(name = "idx_verification_status", columnList = "investigationStatus")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "investigation_status", nullable = false, length = 30)
    @Builder.Default
    private InvestigationStatus investigationStatus = InvestigationStatus.PENDING;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore; // 0 to 100

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel; // LOW, MEDIUM, HIGH

    // Individual signal indicators for transparency
    @Column(name = "tampering_detected", nullable = false)
    private Boolean tamperingDetected;

    @Column(name = "tampering_confidence", nullable = false)
    private Double tamperingConfidence;

    @Column(name = "face_matched", nullable = false)
    private Boolean faceMatched;

    @Column(name = "face_match_confidence", nullable = false)
    private Double faceMatchConfidence;

    // Structured JSON fields (stored as TEXT for database vendor portability)
    @Column(name = "ocr_data_json", columnDefinition = "TEXT")
    private String ocrDataJson;

    @Column(name = "inconsistencies_json", columnDefinition = "TEXT")
    private String inconsistenciesJson;

    @Column(name = "reasons_json", columnDefinition = "TEXT")
    private String reasonsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "investigator_notes", length = 1000)
    private String investigatorNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.investigationStatus == null) {
            this.investigationStatus = InvestigationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

