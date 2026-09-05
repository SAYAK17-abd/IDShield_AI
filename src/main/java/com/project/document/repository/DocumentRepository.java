package com.project.document.repository;

import com.project.document.entity.Document;
import com.project.document.entity.DocumentStatus;
import com.project.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByOwner(User owner, Pageable pageable);
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);
    Optional<Document> findByIdAndOwner(Long id, User owner);
}

