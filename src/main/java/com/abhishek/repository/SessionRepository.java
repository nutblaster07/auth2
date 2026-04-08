package com.abhishek.repository;



import com.abhishek.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * SessionRepository — database operations for user sessions.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Find a session by its token string.
     * This is called on every protected API request to verify the caller.
     * The frontend sends: Authorization: Bearer <sessionToken>
     * We look it up here to identify the user.
     */
    Optional<Session> findBySessionToken(String sessionToken);

    /**
     * Delete a session by its token string.
     * Called during logout — removes the session so it can't be reused.
     */
    void deleteBySessionToken(String sessionToken);

    /**
     * Delete all sessions that have expired.
     * Can be scheduled to run nightly to clean up the sessions table.
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}