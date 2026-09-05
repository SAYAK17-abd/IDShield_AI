package com.project.verification.controller;

import com.project.common.ApiResponse;
import com.project.common.PagedResponse;
import com.project.verification.dto.UpdateInvestigationStatusRequest;
import com.project.verification.dto.VerificationResponseDto;
import com.project.verification.entity.InvestigationStatus;
import com.project.verification.entity.RiskLevel;
import com.project.verification.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Verification Controller.
 * (CRITICAL CODE EXAMPLE 6: Investigator-only endpoints & verification results)
 *
 * Exposes AI screening initiation, result inspection, and human investigator status updates.
 */
@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Verification & Investigation", description = "Endpoints for AI-based document screening, risk analysis, and investigator workflows")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/documents/{documentId}")
    @Operation(summary = "Trigger document verification", description = "Initiates FastAPI AI inspection and calculates transparent risk scores.")
    public ResponseEntity<ApiResponse<VerificationResponseDto>> triggerVerification(
            @PathVariable Long documentId,
            HttpServletRequest request
    ) {
        VerificationResponseDto response = verificationService.triggerVerification(documentId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Document verification analysis completed"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@documentSecurity.canAccessVerification(#id, authentication)")
    @Operation(summary = "Get verification result by ID", description = "IDOR protected: Accessible only to document owner, investigators, or admins.")
    public ResponseEntity<ApiResponse<VerificationResponseDto>> getVerificationById(@PathVariable Long id) {
        VerificationResponseDto result = verificationService.getVerificationById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Get verification by document ID", description = "IDOR protected retrieval of verification findings using document ID.")
    public ResponseEntity<ApiResponse<VerificationResponseDto>> getVerificationByDocumentId(@PathVariable Long documentId) {
        VerificationResponseDto result = verificationService.getVerificationByDocumentId(documentId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "List verification cases", description = "Investigators and admins can view and filter all cases; users only view their own.")
    public ResponseEntity<ApiResponse<PagedResponse<VerificationResponseDto>>> getVerifications(
            @Parameter(description = "Filter by Risk Level (LOW, MEDIUM, HIGH)")
            @RequestParam(value = "riskLevel", required = false) RiskLevel riskLevel,
            @Parameter(description = "Filter by Status (PENDING, PROCESSING, REVIEW_REQUIRED, VERIFIED, REJECTED, FAILED)")
            @RequestParam(value = "status", required = false) InvestigationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<VerificationResponseDto> response =
                PagedResponse.fromPage(verificationService.getVerifications(riskLevel, status, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_INVESTIGATOR', 'ROLE_ADMIN')")
    @Operation(summary = "Update investigation case status", description = "INVESTIGATOR or ADMIN only: Updates case review status and notes.")
    public ResponseEntity<ApiResponse<VerificationResponseDto>> updateInvestigationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvestigationStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        VerificationResponseDto result = verificationService.updateInvestigationStatus(id, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result, "Investigation status updated successfully"));
    }
}

