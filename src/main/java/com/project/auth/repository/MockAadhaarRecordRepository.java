package com.project.auth.repository;

import com.project.auth.entity.MockAadhaarRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockAadhaarRecordRepository extends JpaRepository<MockAadhaarRecord, Long> {
    Optional<MockAadhaarRecord> findByAadhaarNumber(String aadhaarNumber);
    boolean existsByAadhaarNumber(String aadhaarNumber);
    List<MockAadhaarRecord> findByIsActiveTrue();
}
