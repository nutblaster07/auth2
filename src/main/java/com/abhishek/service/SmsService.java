package com.abhishek.service;

import org.springframework.stereotype.Service;

/**
 * SmsService (DEV MODE)
 *
 * This is a MOCK SMS service.
 * Instead of sending real SMS, it prints OTP in console.
 *
 * Use this during development.
 */
@Service
public class SmsService {

    public void sendOtp(String phoneNumber, String otp) {

        // 🔥 Normalize phone (optional safety)
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");

        if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }

        // ✅ Print OTP in console instead of sending SMS
        System.out.println("=================================");
        System.out.println("📱 MOCK SMS SENT");
        System.out.println("Phone: " + cleanPhone);
        System.out.println("OTP: " + otp);
        System.out.println("=================================");
    }
}