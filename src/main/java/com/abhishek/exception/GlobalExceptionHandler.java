package com.abhishek.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — catches ALL exceptions thrown anywhere in the app
 * and converts them into clean JSON error responses.
 *
 * @RestControllerAdvice — this class applies to ALL @RestController classes.
 * Instead of each controller needing its own try-catch, exceptions bubble up
 * to here and are handled in one place.
 *
 * WITHOUT THIS:
 * Any unhandled exception → Spring returns a 500 error with an ugly HTML page
 * or a verbose Spring error JSON that exposes internal details.
 *
 * WITH THIS:
 * Every error returns a consistent JSON format:
 * {
 *   "status": 400,
 *   "message": "OTP has expired. Please request a new one.",
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid on controller parameters.
     * Triggered when @NotBlank, @Email, @Size, etc. fail.
     *
     * Returns HTTP 400 with a map of field → error message:
     * {
     *   "status": 400,
     *   "message": "Validation failed",
     *   "errors": {
     *     "email": "Please enter a valid email address",
     *     "phone": "Phone number is required"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        // Loop through each field that failed validation
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles UserNotFoundException.
     * Returns HTTP 404 — "No account found with this email/phone."
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex) {

        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles InvalidOtpException.
     * Returns HTTP 400 — "Invalid OTP", "OTP expired", "Too many attempts".
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOtp(
            InvalidOtpException ex) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles DuplicateUserException.
     * Returns HTTP 409 Conflict — "Email already registered".
     */
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateUser(
            DuplicateUserException ex) {

        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Catch-all handler for any other unexpected exceptions.
     * Returns HTTP 500 — hides internal details from the client.
     * In production, you'd log the full exception here.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the real error on the server (don't expose it to clients)
        System.err.println("Unexpected error: " + ex.getMessage());
        ex.printStackTrace();

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later."
        );
    }

    /**
     * Helper method to build a consistent error response map.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.value());
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(status).body(response);
    }
}
