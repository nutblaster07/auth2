package com.abhishek.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    @Bean
    public SendGrid sendGrid(@Value("${sendgrid.api.key}") String apiKey) {

        System.out.println("ACTUAL SENDGRID KEY: " + apiKey); // 🔥 MUST PRINT

        return new SendGrid(apiKey);
    }
}