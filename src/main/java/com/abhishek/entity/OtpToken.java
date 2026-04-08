package com.abhishek.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OtpToken — stores OTPs used for LOGIN (not signup).
 *
 * DIFFERENCE FROM PendingUser:
 * PendingUser.otp is used during SIGNUP verification.
 * OtpToken is used when a REGISTERED user logs in via OTP.
 *
 * FLOW:
 * 1. Registered user enters their email or phone on the login page
 * 2. Backend checks the user exists → generates OTP → saves here → sends OTP
 * 3. User enters OTP → backend validates this record → creates a Session
 * 4. This OtpToken record is DELETED after successful verification
 *
 * attempts field:
 * Each wrong OTP increments this. If it reaches maxAttempts (3),
 * the OTP is locked and user must request a new one.
 */
@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The email address or phone number this OTP was sent to.
     * Examples: "user@gmail.com" or "+919876543210"
     */
    @Column(name = "target", nullable = false, length = 255)
    private String target;

    /** The 6-digit OTP code */
    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    /**
     * Whether this was sent via email or phone.
     * Values: "EMAIL" or "PHONE"
     */
    @Column(name = "otp_type", nullable = false, length = 10)
    private String otpType;

    /**
     * Number of failed verification attempts.
     * Starts at 0. If it reaches 3, the OTP is invalidated.
     */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /**
     * When this OTP expires (10 minutes from creation).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
