package com.project.user.dto;

import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Safe User DTO for client responses.
 * Strictly excludes passwordHash, tokens, or security credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String mobileNumber;
    private String employeeId;
    private Role role;
    private UserStatus status;
    private Boolean isMobileVerified;
    private Instant createdAt;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .employeeId(user.getEmployeeId())
                .role(user.getRole())
                .status(user.getStatus())
                .isMobileVerified(user.isMobileVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
