package com.project.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    @NotBlank(message = "Message content is required")
    private String message;

    private List<ChatMessageDto> conversationHistory;
    private String documentType;
    private Map<String, Object> verificationContext;
}
