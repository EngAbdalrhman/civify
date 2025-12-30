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
import com.app.civify.util.CommonResponse;

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
     * Translate a message key to a localized message.
     * If the message looks like a message key (e.g., "subscription.notfound"),
     * it will be translated using the MessageService.
     * Otherwise, the original message is returned.
     *
     * @param message The message or message key
     * @return The localized message
     */
    private String getLocalizedMessage(String message) {
        if (message == null || message.isEmpty()) {
            return messageService.getMessage("error.unknown");
        }
        
        // Check if message looks like a message key (lowercase with dots, no spaces)
        if (message.matches("^[a-z][a-z0-9]*\\.[a-z0-9.]+$")) {
            String translated = messageService.getMessage(message);
            // If translation found (different from key), return it
            if (!translated.equals(message)) {
                return translated;
            }
        }
        
        return message;
    }

    /**
     * Handle service exceptions
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse<Void>> handleServiceException(ServiceException ex, WebRequest request) {
        String requestId = getRequestId(request);
        log.error("SERVICE_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle all uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception ex, WebRequest request) {
        String requestId = getRequestId(request);
        log.error("UNEXPECTED_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(messageService.getMessage("error.unknown")));
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        String requestId = getRequestId(request);
        log.warn("ENTITY_NOT_FOUND | requestId={} | message={}", requestId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? getLocalizedMessage(ex.getMessage())
                : messageService.getMessage("common.notfound");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(errorMessage));
    }

    /**
     * Handle resource already exists exceptions
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<CommonResponse<Void>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        log.warn("Resource already exists: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle bad request exceptions
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Void>> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle unauthorized access exceptions
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<CommonResponse<Void>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        String requestId = getRequestId(request);
        log.warn("UNAUTHORIZED_EXCEPTION | requestId={} | message={}", requestId, ex.getMessage());

        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? getLocalizedMessage(ex.getMessage())
                : messageService.getMessage("common.unauthorized");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(errorMessage));
    }

    /**
     * Handle validation exceptions from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, getLocalizedMessage(errorMessage));
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(messageService.getMessage("validation.failed"), errors));
    }

    /**
     * Handle custom validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        log.warn("Custom validation failed: {}", ex.getErrors());

        // Translate each error message
        Map<String, String> localizedErrors = new HashMap<>();
        if (ex.getErrors() != null) {
            ex.getErrors().forEach((key, value) -> localizedErrors.put(key, getLocalizedMessage(value)));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage()), localizedErrors));
    }

    /**
     * Handle custom authorization exceptions
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<CommonResponse<Void>> handleAuthorizationException(AuthorizationException ex) {
        log.warn("Authorization exception: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Handle custom entity not found exceptions
     */
    @ExceptionHandler(com.app.civify.exception.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleCustomEntityNotFoundException(
            com.app.civify.exception.EntityNotFoundException ex) {
        log.warn("Custom entity not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(getLocalizedMessage(ex.getMessage())));
    }

    /**
     * Helper method to get request ID from headers
     */
    private String getRequestId(WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        return requestId != null ? requestId : "unknown";
    }
}

