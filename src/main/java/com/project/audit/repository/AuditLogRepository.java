package com.project.audit.repository;

import com.project.audit.entity.AuditEventType;
import com.project.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
}

