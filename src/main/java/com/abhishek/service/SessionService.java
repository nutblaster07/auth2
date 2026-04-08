package com.abhishek.service;



import com.abhishek.entity.Session;
import com.abhishek.entity.User;
import com.abhishek.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * SessionService — manages user sessions after successful login.
 *
 * WHAT IS A SESSION?
 * After a user logs in successfully (OTP verified), we create a "session token."
 * This is a random UUID string that acts as a temporary password.
 * The frontend stores it in localStorage and sends it with every API request.
 * The backend checks this token to know who is making requests.
 *
 * SESSION LIFECYCLE:
 * 1. Created: immediately after successful OTP verification
 * 2. Used: frontend sends it as "Authorization: Bearer <token>" header
 * 3. Validated: backend looks up the token, checks it's not expired
 * 4. Destroyed: on logout, or after 7 days (configurable)
 *
 * SECURITY NOTE:
 * This is a simple session token approach. For production at scale,
 * you'd typically use JWT (JSON Web Tokens) instead, which are stateless.
 * But for this project, DB-stored tokens are simpler and more controllable.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    @Value("${app.session.expiryDays:7}")
    private int sessionExpiryDays;

    /**
     * Create a new session for the given user and return the token string.
     *
     * @param user the authenticated user
     * @return the session token string (frontend stores this)
     */
    @Transactional
    public String createSession(User user) {
        // Generate a random, unique token
        // UUID.randomUUID() produces a 36-character string like:
        // "550e8400-e29b-41d4-a716-446655440000"
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        // Combined: 73 chars — very hard to guess

        Session session = Session.builder()
                .user(user)
                .sessionToken(token)
                .expiresAt(LocalDateTime.now().plusDays(sessionExpiryDays))
                .build();

        sessionRepository.save(session);

        return token;
    }

    /**
     * Validate a session token and return the associated User.
     *
     * Checks:
     * 1. Token exists in the database
     * 2. Token has not expired
     *
     * @param token the session token from the Authorization header
     * @return Optional<User> — empty if token is invalid or expired
     */
    public Optional<User> getUserBySessionToken(String token) {
        return sessionRepository.findBySessionToken(token)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(Session::getUser);
    }

    /**
     * Delete a session (called on logout).
     * After this, the token can no longer be used.
     *
     * @param token the session token to invalidate
     */
    @Transactional
    public void deleteSession(String token) {
        sessionRepository.deleteBySessionToken(token);
    }

    /**
     * Clean up all expired sessions from the database.
     * This keeps the sessions table from growing forever.
     *
     * In production, you'd schedule this with @Scheduled(cron = "0 0 2 * * *")
     * to run at 2 AM every night.
     */
    @Transactional
    public void deleteExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}