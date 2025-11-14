package com.app.civify.controller.v1;

import com.app.civify.dto.user.UserMapper;
import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin controller for user management Provides endpoints for filtering and
 * viewing users
 */
@Slf4j
@RestController
// @ApiV1Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Admin endpoints for user management and filtering")
// @PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final CommonService commonService;
    private final UserMapper userMapper;

    @Operation(
            summary = "Get all users with filtering",
            description = "Retrieves all users with optional filtering, sorting, and pagination. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid filter criteria",
                content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/filter")
    public ResponseEntity<PagedResponse<UserResponseDTO>> filterUsers(@Valid @RequestBody InputDataModel inputData) {
        log.info("Admin filtering users with criteria: {}", inputData);

        try {
            // Use CommonService to filter users
            Page<User> userPage = commonService.filterEntities(userRepository, inputData);

            // Convert to DTOs
            List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                    .map(userMapper::toResponseDTO)
                    .collect(Collectors.toList());

            // Create paged response using the static factory method
            PagedResponse<UserResponseDTO> response = new PagedResponse<>();
            response.setContent(userDTOs);
            response.setTotalElements(userPage.getTotalElements());
            response.setTotalPages(userPage.getTotalPages());
            response.setPage(userPage.getNumber());
            response.setSize(userPage.getSize());
            response.setFirst(userPage.isFirst());
            response.setLast(userPage.isLast());
            response.setHasNext(userPage.hasNext());
            response.setHasPrevious(userPage.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error filtering users: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Get all users (simple)",
            description = "Retrieves all users without filtering. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        log.info("Admin fetching all users");

        List<User> users = userRepository.findAll();
        List<UserResponseDTO> userDTOs = users.stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by ID. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        log.info("Admin fetching user with ID: {}", userId);

        return userRepository.findById(userId)
                .map(userMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
