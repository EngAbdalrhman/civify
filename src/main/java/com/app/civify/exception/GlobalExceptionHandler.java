package com.app.civify.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.app.civify.service.i18n.MessageService;
import com.app.civify.util.ApiResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler to ensure consistent error responses across the API
 * Supports internationalization (i18n) for error messages
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /**
     * Handle service exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException ex, WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = "unknown";
        }

        // Log the exception with request ID for ELK correlation
        log.error("SERVICE_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle all uncaught exceptions
     *
     * @param ex The exception
     * @param request The web request
     * @return A standardized error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = "unknown";
        }

        // Log the exception with request ID for ELK correlation
        log.error("UNEXPECTED_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        String errorMessage = messageService.getMessage("error.unknown");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handle entity not found exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = "unknown";
        }

        log.warn("ENTITY_NOT_FOUND | requestId={} | message={}", requestId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Request-ID", requestId)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle resource not found exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        // Use the exception message if it's a specific message, otherwise use generic localized message
        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : messageService.getMessage("common.notfound");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handle resource already exists exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponse<Void>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        log.warn("Resource already exists: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle illegal argument exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle bad request exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle unauthorized access exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = "unknown";
        }

        log.warn("UNAUTHORIZED_EXCEPTION | requestId={} | message={}", requestId, ex.getMessage());

        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : messageService.getMessage("common.unauthorized");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-Request-ID", requestId)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handle validation exceptions from @Valid annotations
     *
     * @param ex The exception
     * @return A standardized error response with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Handle custom validation exceptions
     *
     * @param ex The exception
     * @return A standardized error response with validation errors
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            ValidationException ex) {
        log.warn("Custom validation failed: {}", ex.getErrors());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    /**
     * Handle custom authorization exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationException(AuthorizationException ex) {
        log.warn("Authorization exception: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle custom entity not found exceptions
     *
     * @param ex The exception
     * @return A standardized error response
     */
    @ExceptionHandler(com.app.civify.exception.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Void>> handleCustomEntityNotFoundException(com.app.civify.exception.EntityNotFoundException ex) {
        log.warn("Custom entity not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
