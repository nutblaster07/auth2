package com.abhishek.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PendingUser — temporary record during the two-step signup verification.
 *
 * FLOW:
 * 1. User submits signup form (name, email, phone)
 *    → We save a PendingUser with an email OTP
 * 2. User verifies their email OTP
 *    → We update the same PendingUser with a phone OTP
 * 3. User verifies their phone OTP
 *    → We create a real User record, then DELETE this PendingUser
 *
 * WHY A SEPARATE TABLE?
 * We don't want half-verified users in the main "users" table.
 * This keeps the users table clean — only fully verified accounts live there.
 *
 * tokenType tells us which stage we're at:
 *   "EMAIL" = waiting for email OTP verification
 *   "PHONE" = email done, waiting for phone OTP verification
 */
@Entity
@Table(name = "pending_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Full name entered during signup */
    @Column(name = "name", length = 100)
    private String name;

    /** Email entered during signup */
    @Column(name = "email", length = 255)
    private String email;

    /** Phone number entered during signup */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * The 6-digit OTP for the current verification step.
     * First it holds the email OTP, then the phone OTP.
     */
    @Column(name = "otp", length = 6)
    private String otp;

    /**
     * Which stage of verification we're in.
     * Values: "EMAIL" or "PHONE"
     */
    @Column(name = "token_type", length = 10)
    private String tokenType;

    /**
     * When this OTP expires. Set to NOW + 10 minutes when OTP is generated.
     * If user tries to verify after this time, they get an error.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}