package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.auth.LoginRequest;
import com.jstn9.expensetracker.dto.auth.LoginResponse;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.repository.UserRepository;
import com.jstn9.expensetracker.security.CustomUserDetailsService;
import com.jstn9.expensetracker.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    public LoginResponse login(LoginRequest loginRequestDTO) {
        User user = userRepository.findByUsername(loginRequestDTO.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if(!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new LoginResponse(accessToken, refreshToken, userDetails.getUsername());
    }

    public LoginResponse refreshToken(String refreshToken) {
        if(!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String username = jwtService.getUsernameFromToken(refreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return new LoginResponse(newAccessToken, refreshToken, userDetails.getUsername());
    }
}
