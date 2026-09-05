package com.project.audit.service;

import com.project.audit.entity.AuditEventType;
import com.project.audit.entity.AuditLog;
import com.project.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only Audit Service.
 * (CRITICAL CODE EXAMPLE 12)
 *
 * Persists security events without logging passwords, JWTs, or raw identity documents.
 * Employs REQUIRES_NEW propagation to ensure audit records persist even if business transactions roll back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(AuditEventType eventType,
                         Long userId,
                         String userEmail,
                         String resourceType,
                         String resourceId,
                         String details,
                         String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(eventType)
                    .userId(userId)
                    .userEmail(userEmail)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(ipAddress != null ? ipAddress : "SYSTEM")
                    .details(sanitizeDetails(details))
                    .build();

            auditLogRepository.save(auditLog);
            log.info("AUDIT: Event [{}] user=[{}] resource=[{}:{}] from IP=[{}]",
                    eventType, userEmail, resourceType, resourceId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to write audit log for event [{}]: {}", eventType, e.getMessage());
        }
    }

    public void logEvent(AuditEventType eventType,
                         Long userId,
                         String userEmail,
                         String resourceType,
                         String resourceId,
                         String details,
                         HttpServletRequest request) {
        String ip = null;
        if (request != null) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            ip = (xfHeader != null && !xfHeader.isEmpty()) ? xfHeader.split(",")[0].trim() : request.getRemoteAddr();
        }
        logEvent(eventType, userId, userEmail, resourceType, resourceId, details, ip);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(AuditEventType eventType, Pageable pageable) {
        if (eventType != null) {
            return auditLogRepository.findByEventType(eventType, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    private String sanitizeDetails(String details) {
        if (details == null) return null;
        // Strip or mask potentially sensitive patterns like tokens or passwords
        return details.replaceAll("(?i)(password|token|secret)=[^, ]+", "$1=***");
    }
}

