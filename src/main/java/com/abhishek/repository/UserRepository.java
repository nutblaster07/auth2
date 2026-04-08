package com.abhishek.repository;



import com.abhishek.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository — database operations for the User entity.
 *
 * By extending JpaRepository<User, UUID>, we get these for FREE:
 *   save(user)          — INSERT or UPDATE
 *   findById(id)        — SELECT by primary key
 *   findAll()           — SELECT all
 *   delete(user)        — DELETE
 *   count()             — COUNT(*)
 *   existsById(id)      — EXISTS check
 *   ... and many more
 *
 * The methods below are custom queries. Spring Data JPA generates the
 * SQL automatically from the method name. For example:
 *   findByEmail(email) → SELECT * FROM users WHERE email = ?
 *
 * @Repository — marks this as a Spring-managed data access component
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     * Returns Optional — empty if no user found with that email.
     * Used during: login (check user exists), signup (check duplicate).
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their phone number.
     * Returns Optional — empty if no user found with that phone.
     * Used during: login (check user exists), signup (check duplicate).
     */
    Optional<User> findByPhone(String phone);

    /**
     * Check if any user exists with this email.
     * Returns boolean — faster than findByEmail when you just need exists/not.
     * Used during signup to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    /**
     * Check if any user exists with this phone number.
     * Used during signup to prevent duplicate accounts.
     */
    boolean existsByPhone(String phone);

    Optional<User> findByToken(String token);
}