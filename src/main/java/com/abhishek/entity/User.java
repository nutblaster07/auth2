package com.abhishek.entity;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User — represents a fully verified, registered user.
 *
 * IMPORTANT: A row in this table is created ONLY after BOTH email and phone
 * OTPs are successfully verified during signup. Before that, data lives in
 * the PendingUser table.
 *
 * @Entity   — tells JPA/Hibernate this class maps to a database table
 * @Table    — specifies the exact table name in PostgreSQL ("users")
 *
 * Lombok annotations:
 * @Getter / @Setter — auto-generates getters and setters for all fields
 * @NoArgsConstructor — generates an empty constructor (required by JPA)
 * @AllArgsConstructor — generates a constructor with all fields
 * @Builder — enables builder pattern: User.builder().name("X").build()
 */

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Primary key — UUID (universally unique identifier).
     * @GeneratedValue with UUID strategy = PostgreSQL auto-generates it.
     * UUID is better than auto-increment integers for security
     * (users can't guess other users' IDs).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User's full name. Cannot be null or empty.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * User's email address.
     * unique = true means no two users can have the same email.
     * nullable = false means it's required.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * User's phone number (e.g. "+919876543210").
     * Also unique — one account per phone number.
     */
    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    /**
     * Whether the user's email was verified during signup.
     * Always true for any user in this table, but kept for clarity.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Whether the user's phone was verified during signup.
     * Also always true for any user in this table.
     */
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    /**
     * Timestamp of when this account was created.
     * @CreationTimestamp — Hibernate sets this automatically on INSERT.
     * updatable = false — this value never changes after creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column
    private String token;
}