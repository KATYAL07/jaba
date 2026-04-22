package com.unityeats.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * BusinessException - Custom unchecked exception for business rule violations.
 *
 * WHY A CUSTOM EXCEPTION?
 * Spring's built-in exceptions (like IllegalArgumentException) are generic.
 * By creating our own, we can:
 * 1. Embed the HTTP status code directly in the exception
 * 2. Write targeted @ExceptionHandler methods in GlobalExceptionHandler
 * 3. Provide meaningful messages to the frontend
 *
 * EXAMPLES OF BUSINESS RULE VIOLATIONS:
 * - An NGO tries to accept a listing that's already been accepted
 * - A Volunteer tries to update a delivery they're not assigned to
 * - A Restaurant tries to delete a listing that's already been picked up
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // Convenience constructors for common HTTP statuses
    public static BusinessException notFound(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }
}
