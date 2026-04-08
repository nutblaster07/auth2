package com.abhishek.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * VerifyOtpDto — request body for all OTP verification endpoints:
 *   POST /api/auth/signup/verify-email
 *   POST /api/auth/signup/verify-phone
 *   POST /api/auth/login/email/verify
 *   POST /api/auth/login/phone/verify
 *
 * target = the email address or phone number the OTP was sent to
 * otp    = the 6-digit code the user typed in
 */
@Data
public class VerifyOtpDto {

    @NotBlank(message = "Email or phone is required")
    private String target;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}