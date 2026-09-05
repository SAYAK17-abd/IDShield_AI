package com.project.security;

import com.project.document.entity.Document;
import com.project.document.repository.DocumentRepository;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import com.project.verification.entity.VerificationResult;
import com.project.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * IDOR (Insecure Direct Object Reference) Protection Service.
 * Used in SpEL expressions:
 *   @PreAuthorize("@documentSecurity.canAccessDocument(#id, authentication)")
 *   @PreAuthorize("@documentSecurity.canAccessVerification(#id, authentication)")
 *
 * Verifies that the requesting user owns the resource or holds an elevated role
 * (ROLE_INVESTIGATOR or ROLE_ADMIN).
 */
@Slf4j
@Component("documentSecurity")
@RequiredArgsConstructor
public class DocumentSecurity {

    private final DocumentRepository documentRepository;
    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean canAccessDocument(Long documentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String email = authentication.getName();
        Optional<User> currentUserOpt = userRepository.findByEmail(email);
        if (currentUserOpt.isEmpty()) {
            return false;
        }

        User currentUser = currentUserOpt.get();

        // Investigators and Admins can access all screening documents
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR) {
            return true;
        }

        // Regular users can ONLY access documents they personally uploaded
        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            // Return false so Spring Security treats missing/unauthorized as forbidden/unauthorized
            return false;
        }

        Document document = documentOpt.get();
        boolean isOwner = document.getOwner().getId().equals(currentUser.getId());
        if (!isOwner) {
            log.warn("IDOR attempt blocked: User [{}] attempted to access Document [{}] owned by [{}]",
                    currentUser.getId(), documentId, document.getOwner().getId());
        }
        return isOwner;
    }

    @Transactional(readOnly = true)
    public boolean canAccessVerification(Long verificationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String email = authentication.getName();
        Optional<User> currentUserOpt = userRepository.findByEmail(email);
        if (currentUserOpt.isEmpty()) {
            return false;
        }

        User currentUser = currentUserOpt.get();

        // Investigators and Admins have full access to verification results
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_INVESTIGATOR) {
            return true;
        }

        // Regular users can only access verification results for their own documents
        Optional<VerificationResult> verificationOpt = verificationRepository.findById(verificationId);
        if (verificationOpt.isEmpty()) {
            return false;
        }

        VerificationResult verification = verificationOpt.get();
        boolean isOwner = verification.getDocument().getOwner().getId().equals(currentUser.getId());
        if (!isOwner) {
            log.warn("IDOR attempt blocked: User [{}] attempted to access Verification [{}] owned by [{}]",
                    currentUser.getId(), verificationId, verification.getDocument().getOwner().getId());
        }
        return isOwner;
    }
}

