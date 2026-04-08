package com.abhishek.repository;



import com.abhishek.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * PendingUserRepository — database operations for PendingUser.
 *
 * Used during the signup verification flow.
 * Records here are temporary — they get deleted after both OTPs are verified
 * and the real User account is created.
 */
@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, UUID> {

    /**
     * Find pending signup by email.
     * Used when user submits the email OTP — we look up their pending record.
     */
    Optional<PendingUser> findByEmail(String email);

    /**
     * Find pending signup by phone.
     * Used when user submits the phone OTP — we look up by phone number.
     */
    Optional<PendingUser> findByPhone(String phone);

    /**
     * Delete pending record by email.
     * Called when signup is abandoned or restarted, or after successful creation.
     */
    void deleteByEmail(String email);

    /**
     * Check if there's already a pending signup for this email.
     * If true, we can either update it or reject a second attempt.
     */
    boolean existsByEmail(String email);

    /**
     * Check if there's already a pending signup for this phone.
     */
    boolean existsByPhone(String phone);
}