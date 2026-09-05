package com.project.user.controller;

import com.project.common.ApiResponse;
import com.project.common.PagedResponse;
import com.project.user.dto.UpdateRoleRequest;
import com.project.user.dto.UserDto;
import com.project.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User Management Controller.
 * (CRITICAL CODE EXAMPLE 5: ADMIN-only endpoints)
 *
 * Restricts access strictly to administrators holding ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "User Management (Admin Only)", description = "Administrative endpoints for user role management and profile inspection")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all registered users", description = "ADMIN only: Returns paginated user accounts.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<UserDto> users = PagedResponse.fromPage(userService.getAllUsers(pageable));
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "ADMIN only: Returns specific user profile.")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "ADMIN only: Modifies user authorization role (ROLE_USER, ROLE_INVESTIGATOR, ROLE_ADMIN).")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            HttpServletRequest httpRequest) {
        UserDto updatedUser = userService.updateUserRole(id, request.getRole(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User role updated successfully"));
    }
}

