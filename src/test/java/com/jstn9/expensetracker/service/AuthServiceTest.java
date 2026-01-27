package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.auth.LoginRequest;
import com.jstn9.expensetracker.dto.auth.LoginResponse;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.repository.UserRepository;
import com.jstn9.expensetracker.security.CustomUserDetailsService;
import com.jstn9.expensetracker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_Succeed() {
        // Given
        LoginRequest request = new LoginRequest("testUser", "password");
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("password");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testUser");

        // Programming the behavior of stubs
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "password")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("testUser", response.getUsername());
    }

    @Test
    void login_Fail_UsernameNotFound() {
        // Given
        LoginRequest request = new LoginRequest("badUsername", "password");

        // Behavior stub
        when(userRepository.findByUsername("badUsername")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_Fail_PasswordDoesNotMatch() {
        // Given
        LoginRequest request = new LoginRequest("testUser", "badPassword");
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("encoded_password");

        // Behavior stub
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("badPassword", "encoded_password")).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_Succeed() {
        // Given
        String refreshToken = "refresh-token";
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testUser");

        // Programming the behavior of stubs
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.getUsernameFromToken(refreshToken)).thenReturn("testUser");
        when(customUserDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access-token");

        // When
        LoginResponse response = authService.refreshToken(refreshToken);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("testUser", response.getUsername());
    }

    @Test
    void refreshToken_Fail_BadCredentials() {
        // Given
        String badToken = "bad-token";

        // Behavior stub
        when(jwtService.validateToken(anyString())).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.refreshToken(badToken));

        // Verify that out code crashed and did not proceed further.
        verify(jwtService, times(1)).validateToken(badToken);
        verifyNoInteractions(customUserDetailsService);
    }
}