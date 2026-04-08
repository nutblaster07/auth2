package com.abhishek.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * AuthResponseDto — the response body returned after a successful login.
 *
 * Returned by:
 *   POST /api/auth/login/email/verify  (on success)
 *   POST /api/auth/login/phone/verify  (on success)
 *
 * The frontend receives this JSON:
 * {
 *   "sessionToken": "a1b2c3d4-...",
 *   "userId": "uuid-...",
 *   "name": "Abhishek",
 *   "email": "user@gmail.com",
 *   "message": "Login successful"
 * }
 *
 * The frontend then does:
 *   localStorage.setItem('sessionToken', response.sessionToken)
 *   localStorage.setItem('userName', response.name)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    /** The session token — frontend stores this and sends it with every API request */
    private String sessionToken;

    /** The user's UUID — useful if frontend needs to call user-specific APIs */
    private UUID userId;

    /** User's full name — for displaying "Welcome, Abhishek!" on dashboard */
    private String name;

    /** User's email — for displaying profile info */
    private String email;

    /** A human-readable success message */
    private String message;
}