package com.project.verification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.client.AiClient;
import com.project.ai.dto.AiAnalysisRequest;
import com.project.ai.dto.AiAnalysisResponse;
import com.project.audit.entity.AuditEventType;
import com.project.audit.service.AuditService;
import com.project.document.entity.Document;
import com.project.document.entity.DocumentStatus;
import com.project.document.repository.DocumentRepository;
import com.project.document.service.DocumentService;
import com.project.document.service.FileStorageService;
import com.project.exception.ApiException;
import com.project.exception.ResourceNotFoundException;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import com.project.verification.dto.RiskScoreDetails;
import com.project.verification.dto.UpdateInvestigationStatusRequest;
import com.project.verification.dto.VerificationResponseDto;
import com.project.verification.entity.InvestigationStatus;
import com.project.verification.entity.RiskLevel;
import com.project.verification.entity.VerificationResult;
import com.project.verification.repository.VerificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;

/**
 * Verification Service managing AI analysis orchestration, risk assessment,
 * investigation workflow states, and strict access controls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final AiClient aiClient;
    private final RiskScoringService riskScoringService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public VerificationResponseDto triggerVerification(Long documentId, HttpServletRequest request) {
        Document document = documentService.findAndVerifyAccess(documentId);
        User currentUser = getCurrentAuthenticatedUser();

        log.info("Triggering verification screening for Document ID [{}] by user [{}]", documentId, currentUser.getEmail());

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        auditService.logEvent(
                AuditEventType.VERIFICATION_CREATED,
                currentUser.getId(),
                currentUser.getEmail(),
                "VERIFICATION",
                documentId.toString(),
                "Verification screening initiated",
                request
        );

        try {
            // Read file payload safely for AI inspection
            Resource fileResource = fileStorageService.loadFileAsResource(document.getStoragePath());
            byte[] fileBytes = fileResource.getContentAsByteArray();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            AiAnalysisRequest.AiAnalysisRequestBuilder requestBuilder = AiAnalysisRequest.builder()
                    .documentId(document.getId())
                    .documentType(document.getDocumentType())
                    .originalFilename(document.getOriginalFilename())
                    .mimeType(document.getMimeType())
                    .fileBase64(base64Content);

            // Read selfie payload if present
            if (document.getSelfieStoragePath() != null && !document.getSelfieStoragePath().isBlank()) {
                try {
                    Resource selfieResource = fileStorageService.loadFileAsResource(document.getSelfieStoragePath());
                    byte[] selfieBytes = selfieResource.getContentAsByteArray();
                    requestBuilder.selfieBase64(Base64.getEncoder().encodeToString(selfieBytes));
                    requestBuilder.selfieFilename("reference_selfie");
                    log.info("Attached [{}] bytes of reference selfie to AI request for document [{}]",
                            selfieBytes.length, document.getId());
                } catch (Exception ex) {
                    log.warn("Could not load attached selfie for document [{}]: {}", document.getId(), ex.getMessage());
                }
            }

            AiAnalysisRequest aiRequest = requestBuilder.build();

            // 1. Call internal FastAPI AI service
            AiAnalysisResponse aiResponse = aiClient.analyzeDocument(aiRequest);

            // 2. Calculate transparent risk score and reasons
            RiskScoreDetails riskDetails = riskScoringService.calculateRisk(aiResponse);

            // 3. Determine investigation status based on risk level
            InvestigationStatus status;
            if (riskDetails.getRiskLevel() == RiskLevel.HIGH || riskDetails.getRiskLevel() == RiskLevel.MEDIUM) {
                status = InvestigationStatus.REVIEW_REQUIRED;
            } else {
                status = InvestigationStatus.COMPLETED;
            }

            String ocrJson = (aiResponse.getOcr() != null) ? objectMapper.writeValueAsString(aiResponse.getOcr()) : null;
            String inconsistenciesJson = (aiResponse.getInconsistencies() != null) ? objectMapper.writeValueAsString(aiResponse.getInconsistencies()) : null;
            String reasonsJson = objectMapper.writeValueAsString(riskDetails.getReasons());

            // 4. Save or update verification result
            VerificationResult verification = verificationRepository.findByDocument(document)
                    .orElse(VerificationResult.builder().document(document).build());

            verification.setInvestigationStatus(status);
            verification.setRiskScore(riskDetails.getRiskScore());
            verification.setRiskLevel(riskDetails.getRiskLevel());
            verification.setTamperingDetected(aiResponse.getTampering().getDetected());
            verification.setTamperingConfidence(aiResponse.getTampering().getConfidence());
            verification.setFaceMatched(aiResponse.getFaceVerification().getMatched());
            verification.setFaceMatchConfidence(aiResponse.getFaceVerification().getConfidence());
            verification.setOcrDataJson(ocrJson);
            verification.setInconsistenciesJson(inconsistenciesJson);
            verification.setReasonsJson(reasonsJson);

            VerificationResult savedResult = verificationRepository.save(verification);

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            auditService.logEvent(
                    AuditEventType.VERIFICATION_COMPLETED,
                    currentUser.getId(),
                    currentUser.getEmail(),
                    "VERIFICATION",
                    savedResult.getId().toString(),
                    String.format("Screening complete: Risk=%s (%d), Status=%s",
                            savedResult.getRiskLevel(), savedResult.getRiskScore(), savedResult.getInvestigationStatus()),
                    request
            );

            return VerificationResponseDto.fromEntity(savedResult, objectMapper);

        } catch (Exception ex) {
            log.error("Verification processing failed for Document ID [{}]", documentId, ex);

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            auditService.logEvent(
                    AuditEventType.VERIFICATION_FAILED,
                    currentUser.getId(),
                    currentUser.getEmail(),
                    "VERIFICATION",
                    documentId.toString(),
                    "Screening failed: " + ex.getMessage(),
                    request
            );

            throw new ApiException("Verification processing failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public VerificationResponseDto getVerificationById(Long id) {
        VerificationResult verification = findAndVerifyAccess(id);
        return VerificationResponseDto.fromEntity(verification, objectMapper);
    }

    @Transactional(readOnly = true)
    public VerificationResponseDto getVerificationByDocumentId(Long documentId) {
        documentService.findAndVerifyAccess(documentId);
        VerificationResult verification = verificationRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationResult", "documentId", documentId));
        return VerificationResponseDto.fromEntity(verification, objectMapper);
    }

    @Transactional(readOnly = true)
    public Page<VerificationResponseDto> getVerifications(RiskLevel riskLevel, InvestigationStatus status, Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();

        // Investigators and Admins can query all verifications with optional filters
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR) {
            return verificationRepository.findWithFilters(riskLevel, status, pageable)
                    .map(v -> VerificationResponseDto.fromEntity(v, objectMapper));
        } else {
            // Standard users only see their personal verification results
            return verificationRepository.findByDocumentOwnerId(currentUser.getId(), pageable)
                    .map(v -> VerificationResponseDto.fromEntity(v, objectMapper));
        }
    }

    @Transactional
    public VerificationResponseDto updateInvestigationStatus(
            Long id,
            UpdateInvestigationStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        User reviewer = getCurrentAuthenticatedUser();

        if (reviewer.getRole() != Role.ROLE_INVESTIGATOR && reviewer.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only Investigators and Administrators can modify case investigation status");
        }

        VerificationResult verification = verificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationResult", "id", id));

        InvestigationStatus oldStatus = verification.getInvestigationStatus();
        verification.setInvestigationStatus(request.getStatus());
        verification.setReviewedBy(reviewer);
        if (request.getInvestigatorNotes() != null) {
            verification.setInvestigatorNotes(request.getInvestigatorNotes().trim());
        }

        VerificationResult updated = verificationRepository.save(verification);

        auditService.logEvent(
                AuditEventType.INVESTIGATION_STATUS_CHANGE,
                reviewer.getId(),
                reviewer.getEmail(),
                "VERIFICATION",
                updated.getId().toString(),
                String.format("Status changed from %s to %s. Notes: %s", oldStatus, request.getStatus(), request.getInvestigatorNotes()),
                httpRequest
        );

        log.info("Investigator [{}] updated Verification [{}] status to [{}]", reviewer.getEmail(), id, request.getStatus());
        return VerificationResponseDto.fromEntity(updated, objectMapper);
    }

    public VerificationResult findAndVerifyAccess(Long verificationId) {
        User currentUser = getCurrentAuthenticatedUser();
        VerificationResult verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationResult", "id", verificationId));

        boolean isElevated = currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR;
        boolean isOwner = verification.getDocument().getOwner().getId().equals(currentUser.getId());

        if (!isElevated && !isOwner) {
            log.warn("IDOR attempt: User [{}] denied access to Verification [{}]", currentUser.getEmail(), verificationId);
            throw new AccessDeniedException("You are not authorized to access this verification result");
        }

        return verification;
    }

    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not authenticated", HttpStatus.UNAUTHORIZED));
    }
}

