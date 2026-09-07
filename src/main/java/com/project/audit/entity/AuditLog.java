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
    @Column(name = "event_type", nullable = false, length = 40)
    private AuditEventType eventType;

    @Column(name = "user_id", length = 100)
    private Long userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(name = "resource_type", length = 100)
    private String resourceType; // e.g., "DOCUMENT", "VERIFICATION", "USER"

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

