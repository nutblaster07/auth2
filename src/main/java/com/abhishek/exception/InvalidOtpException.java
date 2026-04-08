package com.abhishek.exception;



/**
 * InvalidOtpException — thrown when OTP verification fails for any reason:
 *   1. OTP code is wrong
 *   2. OTP has expired (older than 10 minutes)
 *   3. Max attempts (3) exceeded
 *   4. No OTP was ever requested for this target
 *
 * GlobalExceptionHandler catches this and returns HTTP 400 Bad Request.
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
