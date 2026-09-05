package com.project.document.controller;

import com.project.common.ApiResponse;
import com.project.common.PagedResponse;
import com.project.document.dto.DocumentResponseDto;
import com.project.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document Controller.
 * (CRITICAL CODE EXAMPLE 8: Secure document upload and authorized retrieval)
 *
 * Implements multipart document uploads, metadata queries, and IDOR protection.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Document Management", description = "Endpoints for secure document upload, metadata inspection, and download")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Secure document upload", description = "Uploads an identity document (PDF, JPEG, PNG). Validates file size, extension, and magic bytes.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @Parameter(description = "Document file (PDF, JPEG, or PNG, max 10MB)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document type (e.g. PASSPORT, NATIONAL_ID, DRIVING_LICENSE)")
            @RequestParam(value = "documentType", defaultValue = "IDENTITY_CARD") String documentType,
            HttpServletRequest request
    ) {
        DocumentResponseDto response = documentService.uploadDocument(file, documentType, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded and validated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@documentSecurity.canAccessDocument(#id, authentication)")
    @Operation(summary = "Get document metadata", description = "Retrieves document metadata by ID. IDOR protected: Only owner, investigator, or admin can access.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocumentById(@PathVariable Long id) {
        DocumentResponseDto document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("@documentSecurity.canAccessDocument(#id, authentication)")
    @Operation(summary = "Download document file", description = "Streams document file binary. IDOR protected.")
    public ResponseEntity<Resource> downloadDocumentFile(@PathVariable Long id) {
        DocumentResponseDto meta = documentService.getDocumentById(id);
        Resource fileResource = documentService.downloadDocumentFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(fileResource);
    }

    @GetMapping
    @Operation(summary = "List documents", description = "Returns paginated documents. Regular users see their own documents; investigators/admins see all.")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDto>>> getDocuments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<DocumentResponseDto> response = PagedResponse.fromPage(documentService.getDocuments(pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

