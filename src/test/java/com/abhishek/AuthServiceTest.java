package com.abhishek;



import com.abhishek.dto.SignupRequestDto;
import com.abhishek.exception.DuplicateUserException;
import com.abhishek.exception.UserNotFoundException;
import com.abhishek.repository.PendingUserRepository;
import com.abhishek.repository.UserRepository;
import com.abhishek.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthServiceTest — unit tests for AuthService.
 *
 * WHAT IS UNIT TESTING?
 * We test one class (AuthService) in isolation.
 * All dependencies (UserRepository, EmailService, etc.) are MOCKED —
 * meaning we control exactly what they return without touching real DB or email.
 *
 * @ExtendWith(MockitoExtension.class) — enables Mockito in JUnit 5
 * @Mock — creates a mock (fake) version of the class
 * @InjectMocks — creates a real AuthService and injects the mocks into it
 *
 * HOW TO RUN:
 *   In IntelliJ: right-click this file → Run 'AuthServiceTest'
 *   Or: mvn test
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PendingUserRepository pendingUserRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthService authService;

    private SignupRequestDto validSignupDto;

    @BeforeEach
    void setUp() {
        // Create a valid signup request used in multiple tests
        validSignupDto = new SignupRequestDto();
        validSignupDto.setName("Abhishek Kumar");
        validSignupDto.setEmail("abhishek@gmail.com");
        validSignupDto.setPhone("+919876543210");
    }

    // ─── SIGNUP TESTS ───────────────────────────────────────────

    @Test
    @DisplayName("Signup succeeds when email and phone are new")
    void initiateSignup_Success() {
        // ARRANGE: Mock repo to say email and phone don't exist
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(pendingUserRepository.existsByEmail(anyString())).thenReturn(false);

        // ACT: Call the method
        assertDoesNotThrow(() -> authService.initiateSignup(validSignupDto));

        // ASSERT: Verify the email was sent
        verify(emailService, times(1)).sendOtp(anyString(), anyString(), eq("signup"));
        // Verify pending user was saved
        verify(pendingUserRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Signup fails when email is already registered")
    void initiateSignup_DuplicateEmail_ThrowsException() {
        // ARRANGE: Mock email as already existing
        when(userRepository.existsByEmail("abhishek@gmail.com")).thenReturn(true);

        // ACT + ASSERT: Expect DuplicateUserException
        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> authService.initiateSignup(validSignupDto)
        );

        assertTrue(exception.getMessage().contains("email already exists"));

        // Verify NO email was sent
        verify(emailService, never()).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Signup fails when phone is already registered")
    void initiateSignup_DuplicatePhone_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> authService.initiateSignup(validSignupDto)
        );

        assertTrue(exception.getMessage().contains("phone number already exists"));
    }

    // ─── LOGIN TESTS ────────────────────────────────────────────

    @Test
    @DisplayName("Email login request succeeds for registered user")
    void requestEmailLoginOtp_RegisteredUser_Success() {
        // ARRANGE: Mock user as existing
        com.abhishek.entity.User mockUser = com.abhishek.entity.User.builder()
                .email("abhishek@gmail.com")
                .name("Abhishek Kumar")
                .build();
        when(userRepository.findByEmail("abhishek@gmail.com"))
                .thenReturn(Optional.of(mockUser));
        when(otpService.generateAndSaveOtp(anyString(), anyString())).thenReturn("123456");

        // ACT
        assertDoesNotThrow(() -> authService.requestEmailLoginOtp("abhishek@gmail.com"));

        // ASSERT: Email OTP was sent
        verify(emailService, times(1)).sendOtp(anyString(), eq("123456"), eq("login"));
    }

    @Test
    @DisplayName("Email login request fails for unregistered email")
    void requestEmailLoginOtp_UnregisteredEmail_ThrowsUserNotFoundException() {
        // ARRANGE: No user with this email
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // ACT + ASSERT: Expect UserNotFoundException
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authService.requestEmailLoginOtp("unknown@gmail.com")
        );

        assertTrue(exception.getMessage().contains("No account found"));

        // Verify NO OTP was generated or sent
        verify(otpService, never()).generateAndSaveOtp(anyString(), anyString());
        verify(emailService, never()).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Phone login request fails for unregistered phone")
    void requestPhoneLoginOtp_UnregisteredPhone_ThrowsUserNotFoundException() {
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> authService.requestPhoneLoginOtp("+919999999999")
        );

        verify(smsService, never()).sendOtp(anyString(), anyString());
    }
}