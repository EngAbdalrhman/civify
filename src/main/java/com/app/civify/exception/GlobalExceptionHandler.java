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
import org.springframework.web.context.request.RequestAttributes;
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
     * Helper to set error message request attribute for logging
     */
    private void setErrorMessageAttribute(WebRequest request, String message) {
        request.setAttribute("API_ERROR_MESSAGE", message, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Handle service exceptions
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse<Void>> handleServiceException(ServiceException ex, WebRequest request) {
        String requestId = getRequestId(request);
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        
        // Capture error for API logging
        setErrorMessageAttribute(request, localizedMessage);
        
        log.error("SERVICE_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Handle all uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception ex, WebRequest request) {
        String requestId = getRequestId(request);
        String errorMessage = messageService.getMessage("error.unknown");
        
        // Capture error for API logging
        setErrorMessageAttribute(request, errorMessage + ": " + ex.getMessage());
        
        log.error("UNEXPECTED_EXCEPTION | requestId={} | message={} | stackTrace={}",
                requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(errorMessage));
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        String requestId = getRequestId(request);
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        
        setErrorMessageAttribute(request, localizedMessage);
        
        log.warn("ENTITY_NOT_FOUND | requestId={} | message={}", requestId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Request-ID", requestId)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? getLocalizedMessage(ex.getMessage())
                : messageService.getMessage("common.notfound");
                
        setErrorMessageAttribute(request, errorMessage);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(errorMessage));
    }

    /**
     * Handle resource already exists exceptions
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<CommonResponse<Void>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex, WebRequest request) {
        log.warn("Resource already exists: {}", ex.getMessage());
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Handle bad request exceptions
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Void>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(localizedMessage));
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
                
        setErrorMessageAttribute(request, errorMessage);

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
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, getLocalizedMessage(errorMessage));
        });

        log.warn("Validation failed: {}", errors);
        
        setErrorMessageAttribute(request, messageService.getMessage("validation.failed"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(messageService.getMessage("validation.failed"), errors));
    }

    /**
     * Handle custom validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse<Map<String, String>>> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Custom validation failed: {}", ex.getErrors());

        // Translate each error message
        Map<String, String> localizedErrors = new HashMap<>();
        if (ex.getErrors() != null) {
            ex.getErrors().forEach((key, value) -> localizedErrors.put(key, getLocalizedMessage(value)));
        }
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(localizedMessage, localizedErrors));
    }

    /**
     * Handle custom authorization exceptions
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<CommonResponse<Void>> handleAuthorizationException(AuthorizationException ex, WebRequest request) {
        log.warn("Authorization exception: {}", ex.getMessage());
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Handle custom entity not found exceptions
     */
    @ExceptionHandler(com.app.civify.exception.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse<Void>> handleCustomEntityNotFoundException(
            com.app.civify.exception.EntityNotFoundException ex, WebRequest request) {
        log.warn("Custom entity not found: {}", ex.getMessage());
        
        String localizedMessage = getLocalizedMessage(ex.getMessage());
        setErrorMessageAttribute(request, localizedMessage);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(localizedMessage));
    }

    /**
     * Helper method to get request ID from headers
     */
    private String getRequestId(WebRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        return requestId != null ? requestId : "unknown";
    }
}

