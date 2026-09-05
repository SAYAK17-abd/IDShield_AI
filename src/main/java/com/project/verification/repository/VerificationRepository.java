package com.project.verification.repository;

import com.project.document.entity.Document;
import com.project.verification.entity.InvestigationStatus;
import com.project.verification.entity.RiskLevel;
import com.project.verification.entity.VerificationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<VerificationResult, Long> {
    Optional<VerificationResult> findByDocument(Document document);
    Optional<VerificationResult> findByDocumentId(Long documentId);

    @Query("SELECT v FROM VerificationResult v WHERE " +
           "(:riskLevel IS NULL OR v.riskLevel = :riskLevel) AND " +
           "(:status IS NULL OR v.investigationStatus = :status)")
    Page<VerificationResult> findWithFilters(@Param("riskLevel") RiskLevel riskLevel,
                                            @Param("status") InvestigationStatus status,
                                            Pageable pageable);

    @Query("SELECT v FROM VerificationResult v WHERE v.document.owner.id = :ownerId")
    Page<VerificationResult> findByDocumentOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}

