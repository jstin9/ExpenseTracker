package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.auth.RegistrationRequest;
import com.jstn9.expensetracker.dto.auth.UserResponse;
import com.jstn9.expensetracker.exception.RoleNotFoundException;
import com.jstn9.expensetracker.exception.UsernameNotFoundException;
import com.jstn9.expensetracker.mapper.UserMapper;
import com.jstn9.expensetracker.model.Role;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.model.enums.RoleNames;
import com.jstn9.expensetracker.repository.RoleRepository;
import com.jstn9.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void save_WhenRoleExist_ThenSaveUser() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("test")
                .email("test@mail.com")
                .password("testPassword")
                .build();

        User user = new User();
        Role role = new Role();
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(request.getUsername());
        userResponse.setEmail(request.getEmail());

        // Behavior stubs
        when(userRepository.existsUserByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsUserByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RoleNames.ROLE_USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.save(request);

        verify(userRepository).save(userCaptor.capture());
        User actualUser = userCaptor.getValue();

        // Then
        assertNotNull(result);
        assertEquals(request.getUsername(), result.getUsername());
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(request.getUsername(), actualUser.getUsername());
        assertEquals(request.getEmail(), actualUser.getEmail());
        assertNotNull(actualUser.getPassword());
        assertEquals("encodedPassword", actualUser.getPassword());
        assertFalse(actualUser.getRoles().isEmpty());

        // Verify
        verify(userRepository).existsUserByUsername(request.getUsername());
        verify(userRepository).existsUserByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(roleRepository).findByName(RoleNames.ROLE_USER);
        verify(profileService).createEmptyProfile(user);
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void save_WhenRoleNotExist_ThenThrowsException() {
        // Given
        RegistrationRequest request = RegistrationRequest.builder()
                .username("test")
                .email("test@mail.com")
                .password("testPassword")
                .build();

        // Behavior stubs
        when(userRepository.existsUserByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsUserByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RoleNames.ROLE_USER)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RoleNotFoundException.class, () -> userService.save(request));

        // Verify
        verify(userRepository).existsUserByUsername(request.getUsername());
        verify(userRepository).existsUserByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(roleRepository).findByName(RoleNames.ROLE_USER);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(profileService, userMapper);
    }

    @Test
    void getCurrentUser_WhenUsernameFound_ThenReturnCurrentUser() {
        // Given
        String username = "testUsername";
        User user = new User();
        user.setUsername(username);

        Authentication authentication = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);

        when(authentication.getName()).thenReturn(username);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);

        // Behavior stubs
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        User result = userService.getCurrentUser();

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());

        // Verify
        verify(authentication).getName();
        verify(context).getAuthentication();
        verify(userRepository).findByUsername(username);

        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_WhenUsernameNotFound_ThenThrowsException() {
        // Given
        String badUsername = "testUsername";

        Authentication authentication = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);

        when(authentication.getName()).thenReturn(badUsername);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);

        // Behavior stubs
        when(userRepository.findByUsername(badUsername)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> userService.getCurrentUser());

        // Verify
        verify(userRepository).findByUsername(badUsername);

        SecurityContextHolder.clearContext();
    }
}