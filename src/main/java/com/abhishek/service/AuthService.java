package com.abhishek.service;



import com.abhishek.dto.AuthResponseDto;
import com.abhishek.dto.SignupRequestDto;
import com.abhishek.entity.PendingUser;
import com.abhishek.entity.User;
import com.abhishek.exception.DuplicateUserException;
import com.abhishek.exception.InvalidOtpException;
import com.abhishek.exception.UserNotFoundException;
import com.abhishek.repository.PendingUserRepository;
import com.abhishek.repository.SessionRepository;
import com.abhishek.repository.UserRepository;
import com.abhishek.entity.Session;
import lombok.RequiredArgsConstructor;
//import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * AuthService — the heart of the authentication system.
 *
 * Contains all business logic for:
 * 1. SIGNUP (3-step process)
 *    Step 1: initiateSignup()    → validate, save PendingUser, send email OTP
 *    Step 2: verifyEmailOtp()    → verify email OTP, send phone OTP
 *    Step 3: verifyPhoneAndCreate() → verify phone OTP, create real User
 *
 * 2. LOGIN via Email OTP (2-step)
 *    Step 1: requestEmailLoginOtp() → check user exists, send email OTP
 *    Step 2: verifyEmailLoginOtp()  → verify OTP, create session, return token
 *
 * 3. LOGIN via Phone OTP (2-step)
 *    Step 1: requestPhoneLoginOtp() → check user exists, send SMS OTP
 *    Step 2: verifyPhoneLoginOtp()  → verify OTP, create session, return token
 *
 * DEPENDENCY INJECTION:
 * All dependencies are injected via constructor (Lombok @RequiredArgsConstructor).
 * This is the recommended Spring approach over @Autowired on fields.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final SessionService sessionService;
    private final SessionRepository sessionRepository;

    @Value("${app.otp.expiryMinutes:10}")
    private int otpExpiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    // ─────────────────────────────────────────────────────────────
    // SIGNUP — STEP 1: Receive form data, send email OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when user submits the signup form (name + email + phone).
     *
     * What happens:
     * 1. Check email isn't already registered
     * 2. Check phone isn't already registered
     * 3. Save data to pending_users table with an email OTP
     * 4. Send email with the OTP to the user
     *
     * @param dto contains name, email, phone from the signup form
     * @throws DuplicateUserException if email or phone is already taken
     */
    @Transactional
    public void initiateSignup(SignupRequestDto dto) {
        // Check for existing registered users with this email
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateUserException(
                    "An account with this email already exists. Please login instead."
            );
        }

        // Check for existing registered users with this phone
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateUserException(
                    "An account with this phone number already exists. Please login instead."
            );
        }

        // If there's already a pending signup for this email, delete it
        // (user might be retrying after an expired OTP)
        if (pendingUserRepository.existsByEmail(dto.getEmail())) {
            pendingUserRepository.deleteByEmail(dto.getEmail());
        }

        // Generate a 6-digit OTP for email verification
        String otp = generateOtp();

        // Save the signup data temporarily in pending_users
        PendingUser pending = PendingUser.builder()
                .name(dto.getName().trim())
                .email(dto.getEmail().toLowerCase().trim())
                .phone(normalizePhone(dto.getPhone()))
                .otp(otp)
                .tokenType("EMAIL")   // waiting for email verification
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        pendingUserRepository.save(pending);

        // Send OTP email FROM noreply@abhishek.com
        emailService.sendOtp(dto.getEmail(), otp, "signup");
    }

    // ─────────────────────────────────────────────────────────────
    // SIGNUP — STEP 2: Verify email OTP, send phone OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when user submits the email OTP they received.
     *
     * What happens:
     * 1. Find pending signup record for this email
     * 2. Validate the OTP (expiry, attempts, correctness)
     * 3. Generate a new phone OTP and update the pending record
     * 4. Send the phone OTP via SMS
     *
     * @param email        the email address (from sessionStorage on frontend)
     * @param submittedOtp the 6-digit code the user typed
     */
    @Transactional
    public void verifyEmailOtp(String email, String submittedOtp) {
        // Find the pending signup for this email
        PendingUser pending = pendingUserRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new InvalidOtpException(
                        "No pending signup found for this email. Please start the signup again."
                ));

        // Make sure we're in the EMAIL verification stage
        if (!"EMAIL".equals(pending.getTokenType())) {
            throw new InvalidOtpException(
                    "Email is already verified. Please verify your phone number."
            );
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pendingUserRepository.delete(pending);
            throw new InvalidOtpException(
                    "OTP has expired. Please start the signup process again."
            );
        }

        // Check if OTP matches
        if (!pending.getOtp().equals(submittedOtp)) {
            throw new InvalidOtpException("Invalid OTP. Please check your email and try again.");
        }

        // Email verified! Now generate phone OTP
        String phoneOtp = generateOtp();

        // Update the pending record for phone verification stage
        pending.setOtp(phoneOtp);
        pending.setTokenType("PHONE");   // now waiting for phone verification
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingUserRepository.save(pending);

        // Send OTP via SMS
        System.out.println("PHONE OTP: " + phoneOtp);
    }

    // ─────────────────────────────────────────────────────────────
    // SIGNUP — STEP 3: Verify phone OTP, create real user account
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when user submits the phone OTP they received via SMS.
     *
     * What happens:
     * 1. Find pending signup by phone number
     * 2. Validate the OTP
     * 3. Create a real User record in the users table
     * 4. Delete the PendingUser record
     *
     * @param phone        the phone number (from sessionStorage on frontend)
     * @param submittedOtp the 6-digit code the user typed
     */


    @Transactional
    public void verifyPhoneAndCreateUser(String phone, String submittedOtp) {

        PendingUser pending = pendingUserRepository.findByPhone(normalizePhone(phone))
                .orElseThrow(() -> new InvalidOtpException(
                        "No pending signup found. Please start the signup process again."
                ));

        if (!"PHONE".equals(pending.getTokenType())) {
            throw new InvalidOtpException(
                    "Please verify your email first before verifying your phone."
            );
        }

        if (LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pendingUserRepository.delete(pending);
            throw new InvalidOtpException(
                    "OTP has expired. Please start the signup process again."
            );
        }

        if (!pending.getOtp().equals(submittedOtp)) {
            throw new InvalidOtpException("Invalid OTP. Please check your SMS and try again.");
        }

        User newUser = User.builder()
                .name(pending.getName())
                .email(pending.getEmail())
                .phone(normalizePhone(pending.getPhone()))
                .emailVerified(true)
                .phoneVerified(true)
                .build();

        userRepository.save(newUser);

        pendingUserRepository.delete(pending);
    }



   /* @Transactional
    public void verifyPhoneAndCreateUser(String phone, String submittedOtp) {
        // Find the pending signup for this phone number
        pendingUserRepository.findByPhone(normalizePhone(phone))
                .orElseThrow(() -> new InvalidOtpException(
                        "No pending signup found. Please start the signup process again."
                ));

        // Make sure we're in the PHONE verification stage
        PendingUser pending = null;
        if (!"PHONE".equals(pending.getTokenType())) {
            throw new InvalidOtpException(
                    "Please verify your email first before verifying your phone."
            );
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pendingUserRepository.delete(pending);
            throw new InvalidOtpException(
                    "OTP has expired. Please start the signup process again."
            );
        }

        // Check if OTP matches
        if (!pending.getOtp().equals(submittedOtp)) {
            throw new InvalidOtpException("Invalid OTP. Please check your SMS and try again.");
        }

        // Both email and phone verified — create the real user account
        User newUser = User.builder()
                .name(pending.getName())
                .email(pending.getEmail())
                .phone(normalizePhone(pending.getPhone()))
                .emailVerified(true)
                .phoneVerified(true)
                .build();

        userRepository.save(newUser);

        // Clean up — delete the temporary pending record
        pendingUserRepository.delete(pending);
    }*/

    // ─────────────────────────────────────────────────────────────
    // LOGIN via EMAIL OTP — Step 1: Request OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when a user enters their email on the login page.
     *
     * CRITICAL RULE: Only registered users can login.
     * If no account exists with this email → throw UserNotFoundException.
     * The frontend shows: "No account found. Please sign up."
     *
     * @param email the email address entered on the login page
     */
    public void requestEmailLoginOtp(String email) {
        String cleanEmail = email.toLowerCase().trim();

        // Check if user is registered — if not, reject immediately
        userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with this email address. Please sign up first."
                ));

        // User exists — generate OTP, save it, send email
        String otp = otpService.generateAndSaveOtp(cleanEmail, "EMAIL");
        emailService.sendOtp(cleanEmail, otp, "login");
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN via EMAIL OTP — Step 2: Verify OTP, create session
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when user submits their login email OTP.
     *
     * @param email        the email address
     * @param submittedOtp the 6-digit code
     * @return AuthResponseDto with session token and user info
     */
    public AuthResponseDto verifyEmailLoginOtp(String email, String submittedOtp) {
        String cleanEmail = email.toLowerCase().trim();

        // Validate OTP (throws InvalidOtpException if invalid)
        otpService.validateOtp(cleanEmail, submittedOtp);

        // OTP is valid — get the user
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        // Create a session and return the token
        String sessionToken = sessionService.createSession(user);

        return AuthResponseDto.builder()
                .sessionToken(sessionToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .message("Login successful. Welcome back, " + user.getName() + "!")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN via PHONE OTP — Step 1: Request OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when a user enters their phone number on the login page.
     * Only registered users can proceed.
     *
     * @param phone the phone number entered on the login page
     */
    public void requestPhoneLoginOtp(String phone) {
        String cleanPhone = normalizePhone(phone);

        // Check if user is registered — reject if not
        userRepository.findByPhone(cleanPhone)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with this phone number. Please sign up first."
                ));

        // User exists — generate OTP, save it, send SMS
        String otp = otpService.generateAndSaveOtp(cleanPhone, "PHONE");
        try {
            smsService.sendOtp(cleanPhone, otp);
        } catch (Exception e) {
            System.out.println("LOGIN PHONE OTP: " + otp);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN via PHONE OTP — Step 2: Verify OTP, create session
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when user submits their login phone OTP.
     *
     * @param phone        the phone number
     * @param submittedOtp the 6-digit code
     * @return AuthResponseDto with session token and user info
     */
    public AuthResponseDto verifyPhoneLoginOtp(String phone, String submittedOtp) {
        String cleanPhone = normalizePhone(phone);

        // Validate OTP
        otpService.validateOtp(cleanPhone, submittedOtp);

        // Get user
        User user = userRepository.findByPhone(cleanPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        // Create session
        String sessionToken = sessionService.createSession(user);

        return AuthResponseDto.builder()
                .sessionToken(sessionToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .message("Login successful. Welcome back, " + user.getName() + "!")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────

    /**
     * Invalidates the session token.
     * After this, any request with the old token will be rejected.
     *
     * @param sessionToken the token from the Authorization header
     */
    public void logout(String sessionToken) {
        sessionService.deleteSession(sessionToken);
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate a random 6-digit OTP using SecureRandom.
     * SecureRandom is cryptographically secure — never use Math.random() for OTPs.
     */
    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }



    private String normalizePhone(String phone) {

        if (phone == null) return null;

        // remove all non-digits
        phone = phone.replaceAll("[^0-9]", "");

        // remove country code (91)
        if (phone.startsWith("91") && phone.length() == 12) {
            phone = phone.substring(2);
        }

        return phone;
    }

    public User getUserFromToken(String token) {

        Session session = sessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        return session.getUser();
    }
}