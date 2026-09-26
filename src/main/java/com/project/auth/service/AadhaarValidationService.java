package com.project.auth.service;

import com.project.auth.entity.MockAadhaarRecord;
import com.project.auth.repository.MockAadhaarRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AadhaarValidationService {

    private final MockAadhaarRecordRepository mockAadhaarRepository;

    // Verhoeff algorithm multiplication table
    private static final int[][] d = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    // Verhoeff algorithm permutation table
    private static final int[][] p = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    /**
     * Validates 12-digit Indian Aadhaar number using standard Verhoeff checksum.
     */
    public boolean validateVerhoeff(String num) {
        if (num == null || !num.matches("^\\d{12}$")) {
            return false;
        }
        int c = 0;
        int[] myArray = new int[num.length()];
        for (int i = 0; i < num.length(); i++) {
            myArray[i] = Integer.parseInt(num.substring(i, i + 1));
        }
        for (int i = 0; i < myArray.length; i++) {
            c = d[c][p[(i % 8)][myArray[myArray.length - i - 1]]];
        }
        return c == 0;
    }

    /**
     * Validates Aadhaar against mock registry database table or Verhoeff algorithm.
     */
    public boolean isValidAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || !aadhaarNumber.matches("^\\d{12}$")) {
            return false;
        }
        // First check mock database registry
        if (mockAadhaarRepository.existsByAadhaarNumber(aadhaarNumber)) {
            return true;
        }
        // Fallback to Verhoeff checksum
        return validateVerhoeff(aadhaarNumber);
    }

    /**
     * Retrieves mock record if present.
     */
    public Optional<MockAadhaarRecord> findRecord(String aadhaarNumber) {
        return mockAadhaarRepository.findByAadhaarNumber(aadhaarNumber);
    }

    /**
     * Returns pre-loaded authorized sandbox Aadhaar pool for UI evaluation.
     */
    public List<MockAadhaarRecord> getSandboxPool() {
        return mockAadhaarRepository.findByIsActiveTrue();
    }
}
