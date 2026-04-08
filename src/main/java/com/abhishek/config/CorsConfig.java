package com.abhishek.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig — Cross-Origin Resource Sharing configuration.
 *
 * WHY THIS IS NEEDED:
 * Browsers block JavaScript from calling a different domain/port than the page itself.
 * Your frontend runs on http://127.0.0.1:5500 (VS Code Live Server).
 * Your backend runs on http://localhost:8080.
 * Without this config, every fetch() call from the frontend would fail with:
 *   "Access to fetch at 'http://localhost:8080' has been blocked by CORS policy"
 *
 * This class tells Spring Boot: "Allow requests from these origins."
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                // Apply this CORS rule to ALL paths starting with /api/
                .addMapping("/**")

                // These are the origins (frontends) we allow.
                // Add your production domain here later (e.g. "https://abhishek.com")
                .allowedOrigins(
                        "http://localhost:5500",
                        "http://127.0.0.1:5500",
                        "http://localhost:3000",
                        "https://frontauth.vercel.app/"                          // in case you use a local Node server later
                )

                // Allow these HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

                // Allow all headers (Content-Type, Authorization, etc.)
                .allowedHeaders("*")

                // Allow the browser to send cookies / Authorization headers
                .allowCredentials(true)

                // How long the browser caches the preflight response (in seconds)
                .maxAge(3600);
    }
}
