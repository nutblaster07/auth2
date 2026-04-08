package com.abhishek.controller;



import com.abhishek.dto.AuthResponseDto;
import com.abhishek.dto.LoginRequestDto;
import com.abhishek.dto.SignupRequestDto;
import com.abhishek.dto.VerifyOtpDto;
import com.abhishek.entity.User;
import com.abhishek.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — exposes all authentication REST API endpoints.
 *
 * BASE URL: http://localhost:8080/api/auth
 *
 * ALL ENDPOINTS:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ SIGNUP                                                          │
 * │  POST /api/auth/signup/initiate      → send email OTP          │
 * │  POST /api/auth/signup/verify-email  → verify email OTP        │
 * │  POST /api/auth/signup/verify-phone  → verify phone, create    │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ LOGIN via EMAIL                                                 │
 * │  POST /api/auth/login/email/request  → send login email OTP    │
 * │  POST /api/auth/login/email/verify   → verify, return token    │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ LOGIN via PHONE                                                 │
 * │  POST /api/auth/login/phone/request  → send login SMS OTP      │
 * │  POST /api/auth/login/phone/verify   → verify, return token    │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ LOGOUT                                                          │
 * │  POST /api/auth/logout               → invalidate session       │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * @RestController = @Controller + @ResponseBody
 *   → every method returns JSON automatically
 *
 * @RequestMapping("/api/auth") → all methods are prefixed with /api/auth
 *
 * @Valid → triggers bean validation on the request body DTO.
 *   If validation fails, MethodArgumentNotValidException is thrown
 *   and handled by GlobalExceptionHandler.
 *
 * ResponseEntity<> → lets us control the HTTP status code (200, 201, 400, etc.)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────
    // SIGNUP ENDPOINTS
    // ─────────────────────────────────────────────────────────────

    /**
     * SIGNUP STEP 1 — Receive signup form data, send email OTP.
     *
     * Request body:
     * {
     *   "name":  "Abhishek Kumar",
     *   "email": "abhishek@gmail.com",
     *   "phone": "+919876543210"
     * }
     *
     * Success response (201 Created):
     * {
     *   "message": "Verification OTP sent to abhishek@gmail.com. Please check your inbox."
     * }
     *
     * Error responses:
     *   400 — validation failed (missing fields, invalid email format, etc.)
     *   409 — email or phone already registered
     */
    @PostMapping("/signup/initiate")
    public ResponseEntity<Map<String, String>> initiateSignup(
            @Valid @RequestBody SignupRequestDto dto) {

        authService.initiateSignup(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Verification OTP sent to " + dto.getEmail()
                                + ". Please check your inbox."
                ));
    }

    /**
     * SIGNUP STEP 2 — Verify the email OTP.
     *
     * Request body:
     * {
     *   "target": "abhishek@gmail.com",
     *   "otp":    "483920"
     * }
     *
     * Success response (200 OK):
     * {
     *   "message": "Email verified! OTP sent to your phone number."
     * }
     *
     * Error responses:
     *   400 — invalid OTP, expired OTP, no pending signup found
     */
    @PostMapping("/signup/verify-email")
    public ResponseEntity<Map<String, String>> verifySignupEmail(
            @Valid @RequestBody VerifyOtpDto dto) {

        authService.verifyEmailOtp(dto.getTarget(), dto.getOtp());

        return ResponseEntity.ok(Map.of(
                "message", "Email verified! An OTP has been sent to your phone number via SMS."
        ));
    }

    /**
     * SIGNUP STEP 3 — Verify the phone OTP and create the user account.
     *
     * Request body:
     * {
     *   "target": "+919876543210",
     *   "otp":    "729104"
     * }
     *
     * Success response (201 Created):
     * {
     *   "message": "Account created successfully! You can now login."
     * }
     *
     * Error responses:
     *   400 — invalid OTP, expired OTP, email not verified first
     */
    @PostMapping("/signup/verify-phone")
    public ResponseEntity<Map<String, String>> verifySignupPhone(
            @Valid @RequestBody VerifyOtpDto dto) {

        authService.verifyPhoneAndCreateUser(dto.getTarget(), dto.getOtp());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Account created successfully! You can now login."
                ));
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN VIA EMAIL OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * EMAIL LOGIN STEP 1 — Check if user exists, send email OTP.
     *
     * Request body:
     * {
     *   "target": "abhishek@gmail.com"
     * }
     *
     * Success response (200 OK):
     * {
     *   "message": "OTP sent to abhishek@gmail.com"
     * }
     *
     * Error responses:
     *   404 — no account with this email (user must sign up)
     */
    @PostMapping("/login/email/request")
    public ResponseEntity<Map<String, String>> requestEmailOtp(
            @Valid @RequestBody LoginRequestDto dto) {

        authService.requestEmailLoginOtp(dto.getTarget());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to " + dto.getTarget() + ". Please check your inbox."
        ));
    }

    /**
     * EMAIL LOGIN STEP 2 — Verify OTP, return session token.
     *
     * Request body:
     * {
     *   "target": "abhishek@gmail.com",
     *   "otp":    "583920"
     * }
     *
     * Success response (200 OK):
     * {
     *   "sessionToken": "uuid-uuid",
     *   "userId": "uuid",
     *   "name": "Abhishek Kumar",
     *   "email": "abhishek@gmail.com",
     *   "message": "Login successful. Welcome back, Abhishek Kumar!"
     * }
     *
     * Error responses:
     *   400 — invalid OTP, expired OTP, too many attempts
     */
    @PostMapping("/login/email/verify")
    public ResponseEntity<AuthResponseDto> verifyEmailOtp(
            @Valid @RequestBody VerifyOtpDto dto) {

        AuthResponseDto response = authService.verifyEmailLoginOtp(
                dto.getTarget(), dto.getOtp()
        );

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN VIA PHONE OTP
    // ─────────────────────────────────────────────────────────────

    /**
     * PHONE LOGIN STEP 1 — Check if user exists, send SMS OTP.
     *
     * Request body:
     * {
     *   "target": "+919876543210"
     * }
     *
     * Success response (200 OK):
     * {
     *   "message": "OTP sent to +919876543210 via SMS."
     * }
     *
     * Error responses:
     *   404 — no account with this phone (user must sign up)
     */
    @PostMapping("/login/phone/request")
    public ResponseEntity<Map<String, String>> requestPhoneOtp(
            @Valid @RequestBody LoginRequestDto dto) {

        authService.requestPhoneLoginOtp(dto.getTarget());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to " + dto.getTarget() + " via SMS."
        ));
    }

    /**
     * PHONE LOGIN STEP 2 — Verify SMS OTP, return session token.
     *
     * Request body:
     * {
     *   "target": "+919876543210",
     *   "otp":    "392847"
     * }
     *
     * Success response: same as email login verify
     * Error responses: same as email login verify
     */
    @PostMapping("/login/phone/verify")
    public ResponseEntity<AuthResponseDto> verifyPhoneOtp(
            @Valid @RequestBody VerifyOtpDto dto) {

        AuthResponseDto response = authService.verifyPhoneLoginOtp(
                dto.getTarget(), dto.getOtp()
        );

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────

    /**
     * LOGOUT — Invalidate the session token.
     *
     * The frontend sends the token in the Authorization header:
     *   Authorization: Bearer a1b2c3d4-...
     *
     * This endpoint extracts the token from the header and deletes the session.
     *
     * Success response (200 OK):
     * {
     *   "message": "Logged out successfully."
     * }
     *
     * @RequestHeader — extracts the Authorization header value.
     * defaultValue = "" means if header is missing, we get empty string (no crash).
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", defaultValue = "") String authHeader) {

        // Header format: "Bearer <token>"
        // We strip the "Bearer " prefix to get just the token
        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // remove "Bearer "
            authService.logout(token);
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ─────────────────────────────────────────────────────────────
    // HEALTH CHECK (useful for testing the server is up)
    // ─────────────────────────────────────────────────────────────

    /**
     * Simple health check endpoint.
     * Visit http://localhost:8080/api/auth/health in your browser.
     * Returns: { "status": "OK", "service": "Abhishek Auth API" }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "Abhishek Auth API"
        ));



    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        User user = authService.getUserFromToken(token);

        return ResponseEntity.ok(user);
    }
}
