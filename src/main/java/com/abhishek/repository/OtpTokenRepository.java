package com.abhishek.repository;



import com.abhishek.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OtpTokenRepository — database operations for login OTP tokens.
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    /**
     * Find the most recently created OTP for a given target (email or phone).
     *
     * WHY "TopBy...OrderByCreatedAtDesc"?
     * A user might click "resend OTP" — this creates a second record.
     * We always want to validate against the LATEST OTP, not an old one.
     * "Top" = LIMIT 1, "OrderByCreatedAtDesc" = newest first.
     *
     * Generated SQL: SELECT * FROM otp_tokens WHERE target = ? ORDER BY created_at DESC LIMIT 1
     */
    Optional<OtpToken> findTopByTargetOrderByCreatedAtDesc(String target);

    /**
     * Delete all OTPs for a given target.
     * Called after successful verification to clean up.
     * Also useful when user requests a new OTP — delete old ones first.
     */
    void deleteByTarget(String target);

    /**
     * Delete all expired OTP records.
     * Can be run periodically to keep the table clean.
     * expiresAtBefore(now) → WHERE expires_at < now
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
