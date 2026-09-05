package com.project.document.service;

import com.project.audit.entity.AuditEventType;
import com.project.audit.service.AuditService;
import com.project.document.dto.DocumentResponseDto;
import com.project.document.entity.Document;
import com.project.document.entity.DocumentStatus;
import com.project.document.repository.DocumentRepository;
import com.project.exception.ApiException;
import com.project.exception.ResourceNotFoundException;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service managing document upload, validation, retrieval, and IDOR enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public DocumentResponseDto uploadDocument(MultipartFile file, String documentType, HttpServletRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        // 1. Strict validation (size, extension, MIME type, magic bytes)
        fileValidator.validate(file);

        String originalFilename = file.getOriginalFilename();
        String extension = fileValidator.getFileExtension(originalFilename);

        // 2. Generate a secure random server-side storage filename (UUIDv4)
        String storedFilename = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            // 3. Store file securely outside the webroot
            String storagePath = fileStorageService.storeFile(file, storedFilename);
            String sha256Checksum = fileStorageService.calculateSha256(file);

            // 4. Save document metadata
            Document document = Document.builder()
                    .owner(currentUser)
                    .documentType(documentType != null && !documentType.isBlank() ? documentType.toUpperCase().trim() : "IDENTITY_CARD")
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .storagePath(storagePath)
                    .fileSizeBytes(file.getSize())
                    .mimeType(file.getContentType())
                    .sha256Checksum(sha256Checksum)
                    .status(DocumentStatus.PENDING)
                    .build();

            Document savedDocument = documentRepository.save(document);

            // 5. Audit log event (never logging document contents)
            auditService.logEvent(
                    AuditEventType.DOCUMENT_UPLOAD,
                    currentUser.getId(),
                    currentUser.getEmail(),
                    "DOCUMENT",
                    savedDocument.getId().toString(),
                    String.format("Uploaded document: type=%s, size=%d bytes", savedDocument.getDocumentType(), savedDocument.getFileSizeBytes()),
                    request
            );

            log.info("Document [{}] securely uploaded by user [{}]", savedDocument.getId(), currentUser.getEmail());
            return DocumentResponseDto.fromEntity(savedDocument);

        } catch (IOException e) {
            log.error("Failed to store uploaded document", e);
            throw new ApiException("Failed to save uploaded file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public DocumentResponseDto getDocumentById(Long id) {
        Document document = findAndVerifyAccess(id);
        return DocumentResponseDto.fromEntity(document);
    }

    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(Long id) {
        Document document = findAndVerifyAccess(id);
        return fileStorageService.loadFileAsResource(document.getStoragePath());
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getDocuments(Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();

        // Admins and Investigators can review all documents; regular users see only their own
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR) {
            return documentRepository.findAll(pageable).map(DocumentResponseDto::fromEntity);
        } else {
            return documentRepository.findByOwner(currentUser, pageable).map(DocumentResponseDto::fromEntity);
        }
    }

    /**
     * Defense-in-depth IDOR verification helper.
     */
    public Document findAndVerifyAccess(Long documentId) {
        User currentUser = getCurrentAuthenticatedUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        boolean isElevated = currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR;
        boolean isOwner = document.getOwner().getId().equals(currentUser.getId());

        if (!isElevated && !isOwner) {
            log.warn("IDOR violation detected: User [{}] attempted to access unauthorized document [{}]",
                    currentUser.getEmail(), documentId);
            throw new AccessDeniedException("You are not authorized to access this document");
        }

        return document;
    }

    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not authenticated", HttpStatus.UNAUTHORIZED));
    }
}

