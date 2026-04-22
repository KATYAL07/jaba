package com.unityeats.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ApiError - Standard error response envelope.
 *
 * WHY A STANDARD ERROR FORMAT?
 * The frontend needs predictable JSON to display error messages.
 * If errors have inconsistent shapes, the JS error handling becomes complex.
 *
 * All errors from our API will return:
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "fieldErrors": {
 *     "foodName": "Food name must only contain letters and spaces",
 *     "quantity": "Quantity must be at least 1"
 *   },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors; // field name -> error message
    private LocalDateTime timestamp;
}
