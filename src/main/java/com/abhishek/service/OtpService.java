package com.abhishek.service;



import com.abhishek.entity.OtpToken;
import com.abhishek.exception.InvalidOtpException;
import com.abhishek.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OtpService — handles all OTP generation and validation for LOGIN.
 *
 * (Signup OTPs are managed directly inside AuthService using PendingUser.)
 *
 * SECURITY NOTES:
 * - We use SecureRandom (not Math.random) — it's cryptographically secure.
 * - OTPs expire after 10 minutes (configurable).
 * - Max 3 wrong attempts before the OTP is locked.
 * - Old OTPs for the same target are deleted before issuing a new one.
 *
 * @Service — marks this as a Spring service component (business logic layer)
 * @RequiredArgsConstructor — Lombok: generates a constructor for all final fields,
 *   which Spring uses for dependency injection (no need for @Autowired)
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    // Read from application.properties
    @Value("${app.otp.expiryMinutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.otp.maxAttempts:3}")
    private int maxAttempts;

    // SecureRandom is thread-safe and cryptographically strong
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new 6-digit OTP for the given target (email or phone).
     *
     * Steps:
     * 1. Delete any existing OTP for this target (clean slate)
     * 2. Generate a random 6-digit number
     * 3. Save it to otp_tokens table with expiry time
     * 4. Return the OTP string so the caller can send it via email/SMS
     *
     * @param target  the email or phone number
     * @param otpType "EMAIL" or "PHONE"
     * @return the 6-digit OTP string (e.g. "483920")
     */
    @Transactional
    public String generateAndSaveOtp(String target, String otpType) {
        // Delete any previous OTP for this target (in case of resend)
        otpTokenRepository.deleteByTarget(target);

        // Generate 6-digit number: 100000 to 999999
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpNumber);

        // Build and save the OTP token record
        OtpToken token = OtpToken.builder()
                .target(target)
                .otp(otp)
                .otpType(otpType)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpTokenRepository.save(token);

        return otp;
    }

    /**
     * Validate the OTP submitted by the user during login.
     *
     * Checks (in order):
     * 1. Does an OTP record exist for this target? (they might not have requested one)
     * 2. Has the max attempts been exceeded?
     * 3. Has the OTP expired?
     * 4. Does the submitted OTP match the stored one?
     *
     * If valid → delete the OTP record (one-time use)
     * If invalid → increment attempts and throw InvalidOtpException
     *
     * @param target      the email or phone number
     * @param submittedOtp the code the user typed in
     * @throws InvalidOtpException if any check fails
     */
    @Transactional
    public void validateOtp(String target, String submittedOtp) {
        // Find the most recently issued OTP for this target
        OtpToken token = otpTokenRepository
                .findTopByTargetOrderByCreatedAtDesc(target)
                .orElseThrow(() -> new InvalidOtpException(
                        "No OTP was requested for this account. Please request a new one."
                ));

        // Check: too many wrong attempts?
        if (token.getAttempts() >= maxAttempts) {
            throw new InvalidOtpException(
                    "Too many failed attempts. Please request a new OTP."
            );
        }

        // Check: OTP expired?
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            // Delete the expired token so they can't keep trying
            otpTokenRepository.delete(token);
            throw new InvalidOtpException(
                    "OTP has expired. Please request a new one."
            );
        }

        // Check: wrong OTP code?
        if (!token.getOtp().equals(submittedOtp)) {
            // Increment the attempt counter
            token.setAttempts(token.getAttempts() + 1);
            otpTokenRepository.save(token);

            int remainingAttempts = maxAttempts - token.getAttempts();
            throw new InvalidOtpException(
                    "Invalid OTP. " + remainingAttempts + " attempt(s) remaining."
            );
        }

        // OTP is valid — delete it so it can't be reused
        otpTokenRepository.delete(token);
    }
}
