package com.abhishek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AuthApplication — the entry point of the entire Spring Boot app.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   1. @Configuration       — marks this as a source of bean definitions
 *   2. @EnableAutoConfiguration — tells Spring Boot to auto-configure based on
 *                                 what's on the classpath (e.g. JPA, Mail, Web)
 *   3. @ComponentScan       — scans this package and all sub-packages for
 *                             @Component, @Service, @Repository, @Controller, etc.
 *
 * HOW TO RUN:
 *   In IntelliJ: right-click this file → Run 'AuthApplication'
 *   Or: mvn spring-boot:run
 *   App starts at: http://localhost:8080
 */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}