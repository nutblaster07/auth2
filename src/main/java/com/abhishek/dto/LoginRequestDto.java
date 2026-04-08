package com.abhishek.dto;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LoginRequestDto — request body for login OTP request endpoints:
 *   POST /api/auth/login/email/request  → body: { "target": "user@gmail.com" }
 *   POST /api/auth/login/phone/request  → body: { "target": "+919876543210" }
 *
 * The backend checks if a user with this email/phone exists.
 * If NOT registered → throw UserNotFoundException (don't send OTP)
 * If registered → send OTP to their email/phone
 */
@Data
public class LoginRequestDto {

    @NotBlank(message = "Email or phone number is required")
    private String target;
}