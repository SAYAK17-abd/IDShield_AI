package com.project.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only AuditLog JPA entity.
 * Records business-critical security and verification events without sensitive PII.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditEventType eventType;

    @Column(length = 100)
    private Long userId;

    @Column(length = 150)
    private String userEmail;

    @Column(length = 100)
    private String resourceType; // e.g., "DOCUMENT", "VERIFICATION", "USER"

    @Column(length = 100)
    private String resourceId;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

