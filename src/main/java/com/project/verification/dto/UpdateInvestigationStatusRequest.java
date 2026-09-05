package com.project.verification.dto;

import com.project.verification.entity.InvestigationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestigationStatusRequest {

    @NotNull(message = "Investigation status is required")
    private InvestigationStatus status;

    @Size(max = 1000, message = "Investigator notes cannot exceed 1000 characters")
    private String investigatorNotes;
}

