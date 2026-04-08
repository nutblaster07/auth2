package com.abhishek.exception;



/**
 * DuplicateUserException — thrown during signup when:
 *   1. A registered user already exists with the given email, OR
 *   2. A registered user already exists with the given phone number
 *
 * GlobalExceptionHandler catches this and returns HTTP 409 Conflict.
 *
 * WHY SEPARATE FROM A GENERIC EXCEPTION?
 * The frontend can check for 409 status and show:
 * "This email is already registered. Please login instead."
 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
