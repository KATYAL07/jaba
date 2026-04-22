package com.unityeats.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - The "catch-all" for all unhandled exceptions.
 *
 * @RestControllerAdvice:
 * A combination of @ControllerAdvice + @ResponseBody.
 * - @ControllerAdvice: Makes this class apply globally to ALL controllers
 * - @ResponseBody: Ensures return values are serialized to JSON
 *
 * HOW IT WORKS:
 * When any controller method throws an exception, Spring checks this class
 * for a matching @ExceptionHandler. If found, the handler method runs and
 * its return value becomes the HTTP response.
 *
 * WITHOUT THIS CLASS: Spring would return a generic Whitelabel Error Page
 * or an ugly internal error response that the frontend can't parse.
 *
 * WITH THIS CLASS: The frontend always receives clean, consistent JSON.
 *
 * HANDLER PRIORITY: More specific exception types take priority.
 * MethodArgumentNotValidException > BusinessException > Exception
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle @Valid annotation failures on DTOs.
     *
     * This is the MOST IMPORTANT handler for our validation-heavy application.
     * When @Valid fails on a DTO (e.g., FoodListingRequest, RegisterRequest),
     * Spring throws MethodArgumentNotValidException containing ALL validation errors.
     *
     * EXAMPLE TRIGGER:
     * POST /api/food with body: {"foodName": "Rice123", "quantity": -5}
     *
     * EXAMPLE RESPONSE:
     * {
     *   "status": 400,
     *   "error": "Validation Failed",
     *   "message": "Input validation failed. Please fix the errors below.",
     *   "fieldErrors": {
     *     "foodName": "Food name must only contain letters and spaces",
     *     "quantity": "Quantity must be at least 1"
     *   },
     *   "timestamp": "2024-01-15T10:30:00"
     * }
     *
     * WHY COLLECT ALL ERRORS?
     * If we only return the first error, the user fixes it, submits again,
     * then gets another error. Poor UX. We return ALL errors at once so the
     * frontend can highlight every invalid field simultaneously.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect all field errors into a Map<fieldName, errorMessage>
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // Each error is a FieldError (wrong field value) or ObjectError (class-level)
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                // Object-level errors (e.g., from class-level @Valid constraints)
                fieldErrors.put("general", error.getDefaultMessage());
            }
        });

        log.warn("Validation failed: {}", fieldErrors);

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed. Please fix the highlighted errors.",
                fieldErrors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle our custom BusinessException.
     * This catches logical errors like "listing already accepted", "not authorized", etc.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ApiError apiError = new ApiError(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.status(ex.getStatus()).body(apiError);
    }

    /**
     * Handle authentication failures (wrong email/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        // IMPORTANT: Don't reveal whether the email or password was wrong.
        // Saying "Email not found" helps attackers enumerate valid emails.
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                "Invalid email or password. Please check your credentials.",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    /**
     * Handle access denied errors (authenticated but wrong role).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "You do not have permission to perform this action.",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    /**
     * Handle user not found during authentication.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFound(UsernameNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                "Invalid email or password.",
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    /**
     * Catch-all handler for any unexpected exceptions.
     *
     * WHY LOG AT ERROR LEVEL?
     * Unlike the above handlers (expected/handled cases logged at WARN),
     * reaching this handler means something unexpected went wrong.
     * Log the full stack trace so developers can investigate.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);

        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}
