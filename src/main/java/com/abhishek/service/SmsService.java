package com.abhishek.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class SmsService {

    @Value("${app.sms.fast2sms.apiKey}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void sendOtp(String phoneNumber, String otp) {

        String cleanPhone = normalizePhone(phoneNumber);

        String message = otp + " is your Authenticator verification code. "
                + "Valid for 10 minutes. Do not share.";

        String url = "https://www.fast2sms.com/dev/bulkV2"
                + "?authorization=" + apiKey
                + "&route=q"
                + "&message=" + urlEncode(message)
                + "&language=english"
                + "&flash=0"
                + "&numbers=" + cleanPhone;

        System.out.println("Sending SMS to: " + cleanPhone);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("cache-control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Fast2SMS [" + response.statusCode() + "]: " + response.body());

            if (response.statusCode() != 200
                    || response.body().contains("\"return\":false")) {
                throw new RuntimeException("Fast2SMS failed: " + response.body());
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS. Please try again.", e);
        }
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (digits.length() != 10) {
            throw new RuntimeException("Invalid phone number. Got: " + digits);
        }
        return digits;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}