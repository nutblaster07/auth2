package com.abhishek.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * SignupRequestDto — the request body for POST /api/auth/signup/initiate
 *
 * WHY DTOs?
 * We never expose our @Entity classes directly to the API.
 * DTOs are simple data containers for what comes IN (request) or goes OUT (response).
 * They also let us add validation annotations without polluting the entity.
 *
 * @Data (Lombok) = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
 *
 * Validation annotations (@NotBlank, @Email, etc.) are checked automatically
 * by Spring when you put @Valid on the controller method parameter.
 * If validation fails, Spring throws MethodArgumentNotValidException,
 * which our GlobalExceptionHandler catches and returns a nice error JSON.
 */
@Data
public class SignupRequestDto {

    /**
     * @NotBlank — rejects null, empty string, and whitespace-only strings
     * message — the error message returned if validation fails
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * @Email — validates proper email format (x@y.z)
     * @NotBlank — also ensures it's not empty
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    /**
     * @Pattern — validates against a regex.
     * This regex accepts: +919876543210 or 9876543210 (10 digits)
     * Adjust the pattern to match your country's phone format.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[+]?[0-9]{10,15}$",
            message = "Please enter a valid phone number (10-15 digits, optional + prefix)"
    )
    private String phone;
}