package com.project.user.service;

import com.project.audit.entity.AuditEventType;
import com.project.audit.service.AuditService;
import com.project.exception.ResourceNotFoundException;
import com.project.user.dto.UserDto;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for administrative user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUserRole(Long userId, Role newRole, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role previousRole = user.getRole();
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        auditService.logEvent(
                AuditEventType.ROLE_CHANGE,
                userId,
                user.getEmail(),
                "USER",
                userId.toString(),
                String.format("Role changed from %s to %s by admin %s", previousRole, newRole, adminEmail),
                httpRequest
        );

        log.info("Admin [{}] changed role for user [{}] to [{}]", adminEmail, user.getEmail(), newRole);
        return UserDto.fromEntity(updatedUser);
    }
}

