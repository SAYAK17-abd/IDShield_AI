package com.project.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.client.AiClient;
import com.project.chat.dto.ChatRequestDto;
import com.project.chat.dto.ChatResponseDto;
import com.project.common.ApiResponse;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import com.project.verification.entity.VerificationResult;
import com.project.verification.repository.VerificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * AI Case Assistant Chat Controller.
 * Provides pre-upload guidance, explainable risk breakdowns, and statutory document rules.
 * Automatically enriches inquiries with the authenticated user's verification report context.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI Case Assistant", description = "Context-aware conversational assistant for identity verification guidance and report explanations")
public class ChatController {

    private final AiClient aiClient;
    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Ask AI Assistant", description = "Submits questions to the AI assistant with automatic document context injection.")
    public ResponseEntity<ApiResponse<ChatResponseDto>> askAssistant(@Valid @RequestBody ChatRequestDto request) {
        // If authenticated, automatically inject the user's latest verification case context
        injectUserVerificationContextIfAvailable(request);

        ChatResponseDto response = aiClient.chatWithAssistant(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void injectUserVerificationContextIfAvailable(ChatRequestDto request) {
        if (request.getVerificationContext() != null && !request.getVerificationContext().isEmpty()) {
            return; // Context already explicitly provided by client
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email)
                .or(() -> userRepository.findByMobileNumber(email))
                .or(() -> userRepository.findByEmployeeId(email.toUpperCase()));

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        Optional<VerificationResult> latestVerification = verificationRepository.findFirstByDocumentOwnerIdOrderByCreatedAtDesc(user.getId());

        if (latestVerification.isPresent()) {
            VerificationResult v = latestVerification.get();
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("riskScore", v.getRiskScore());
            ctx.put("riskLevel", v.getRiskLevel() != null ? v.getRiskLevel().name() : "LOW");
            ctx.put("investigationStatus", v.getInvestigationStatus() != null ? v.getInvestigationStatus().name() : "COMPLETED");
            ctx.put("tamperingDetected", v.getTamperingDetected());
            ctx.put("tamperingConfidence", v.getTamperingConfidence());
            ctx.put("faceMatched", v.getFaceMatched());
            ctx.put("faceMatchConfidence", v.getFaceMatchConfidence());

            if (v.getDocument() != null) {
                ctx.put("documentType", v.getDocument().getDocumentType());
                if (request.getDocumentType() == null || request.getDocumentType().isBlank()) {
                    request.setDocumentType(v.getDocument().getDocumentType());
                }
            }

            try {
                if (v.getReasonsJson() != null && !v.getReasonsJson().isBlank()) {
                    List<String> reasons = objectMapper.readValue(v.getReasonsJson(), new TypeReference<List<String>>() {});
                    ctx.put("reasons", reasons);
                }
                if (v.getInconsistenciesJson() != null && !v.getInconsistenciesJson().isBlank()) {
                    List<String> inconsistencies = objectMapper.readValue(v.getInconsistenciesJson(), new TypeReference<List<String>>() {});
                    ctx.put("inconsistencies", inconsistencies);
                }
            } catch (Exception e) {
                log.warn("Failed to parse verification reasons JSON: {}", e.getMessage());
            }

            request.setVerificationContext(ctx);
            log.info("Injected verification case #{} context for user [{}] into AI chat inquiry", v.getId(), user.getEmail());
        }
    }
}
