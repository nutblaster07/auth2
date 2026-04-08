package com.abhishek.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Session — represents an active login session for a user.
 *
 * WHAT IS A SESSION TOKEN?
 * After a user successfully verifies their login OTP, we generate a random
 * token (UUID) and store it here. We send this token to the frontend.
 * The frontend stores it in localStorage and sends it with every API request.
 * The backend checks this token to know WHO is making the request.
 *
 * FLOW:
 * 1. User verifies login OTP successfully
 * 2. Backend creates a Session record with a unique sessionToken
 * 3. sessionToken is returned to frontend in the response
 * 4. Frontend saves it: localStorage.setItem('sessionToken', token)
 * 5. Every protected API call includes: Authorization: Bearer <token>
 * 6. Session expires after 7 days (configurable in application.properties)
 *
 * ON LOGOUT:
 * The session record is deleted from this table.
 * If someone tries to use the old token after logout, it won't be found → rejected.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Foreign key to the users table.
     * @ManyToOne — many sessions can belong to one user
     * (e.g. user logged in on phone and laptop = 2 sessions)
     * FetchType.LAZY — don't load the User object unless explicitly asked
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The secret token sent to the frontend.
     * unique = true ensures no two sessions share the same token.
     * Generated as UUID.randomUUID().toString() in SessionService.
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 128)
    private String sessionToken;

    /**
     * When this session expires (default: 7 days from creation).
     * After this time, the user must log in again.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}