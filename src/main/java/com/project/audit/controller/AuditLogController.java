package com.project.audit.controller;

import com.project.audit.entity.AuditEventType;
import com.project.audit.entity.AuditLog;
import com.project.audit.service.AuditService;
import com.project.common.ApiResponse;
import com.project.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit Log Controller.
 * Restricted strictly to system administrators.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Audit Logs (Admin Only)", description = "Append-only security and administrative audit trail inspection")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Query audit logs", description = "ADMIN only: Returns paginated security and system audit events.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getAuditLogs(
            @Parameter(description = "Filter by event type")
            @RequestParam(value = "eventType", required = false) AuditEventType eventType,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<AuditLog> logs = PagedResponse.fromPage(auditService.getLogs(eventType, pageable));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

