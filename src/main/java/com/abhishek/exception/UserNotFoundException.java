package com.abhishek.exception;



/**
 * UserNotFoundException — thrown when login is attempted by an unregistered user.
 *
 * WHEN IS THIS THROWN?
 * POST /api/auth/login/email/request — if no user with that email exists
 * POST /api/auth/login/phone/request — if no user with that phone exists
 *
 * WHY A CUSTOM EXCEPTION?
 * Using a specific exception lets our GlobalExceptionHandler catch it
 * and return HTTP 404 with a clear message, instead of a generic 500 error.
 *
 * RuntimeException — we extend this so we don't need try-catch everywhere.
 * Spring's exception handler will catch it globally.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
