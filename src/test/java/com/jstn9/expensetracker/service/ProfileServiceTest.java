package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.profile.ProfileRequest;
import com.jstn9.expensetracker.dto.profile.ProfileResponse;
import com.jstn9.expensetracker.exception.ProfileNotFilledException;
import com.jstn9.expensetracker.exception.ProfileNotFoundException;
import com.jstn9.expensetracker.mapper.ProfileMapper;
import com.jstn9.expensetracker.model.Profile;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.model.enums.CurrencyType;
import com.jstn9.expensetracker.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfile_WhenProfileFound_ThenReturnsProfile() {
        // Given
        User user = new User();
        Profile profile = Profile.builder()
                .id(1L)
                .name("testProfile")
                .build();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileMapper.toProfileResponse(profile))
                .thenReturn(ProfileResponse.builder().id(profile.getId()).name(profile.getName()).build());

        // When
        ProfileResponse response = profileService.getProfile();

        // Then
        assertNotNull(response);
        assertEquals(profile.getId(), response.getId());
        assertEquals(profile.getName(), response.getName());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).findByUser(user);
        verify(profileMapper).toProfileResponse(profile);
    }


    @Test
    void getProfile_WhenProfileNotFound_ThenThrowsException(){
        // Given
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProfileNotFoundException.class, () -> profileService.getProfile());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).findByUser(user);
        verifyNoInteractions(profileMapper);
    }

    @Test
    void createEmptyProfile_WhenCalled_ThenProfileWithUserIsSaved() {
        // Given
        User user = User.builder()
                .id(1L)
                .username("testName")
                .build();

        // When
        profileService.createEmptyProfile(user);

        // Verify
        verify(profileRepository).save(argThat(profile ->
                profile.getUser().equals(user)
                && profile.getName().isEmpty()
                && profile.getBalance().equals(BigDecimal.ZERO)
                && profile.getMonthSalary().equals(BigDecimal.ZERO)
                && profile.getCurrencyType().equals(CurrencyType.EUR)
        ));
    }

    @Test
    void saveProfile() {
        // Given
        Profile profile = new Profile();

        // When
        profileService.saveProfile(profile);

        // Verify
        verify(profileRepository).save(profile);
    }

    @Test
    void updateProfile_WhenProfileFound_ThenReturnsUpdatedProfile() {
        // Given
        ProfileRequest request = ProfileRequest.builder()
                .name("newName")
                .balance(BigDecimal.valueOf(1000))
                .monthSalary(BigDecimal.valueOf(2000))
                .currencyType(CurrencyType.EUR)
                .build();

        User user = new User();
        Profile profile = Profile.builder()
                .id(1L)
                .user(user)
                .build();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(profileMapper.toProfileResponse(profile))
                .thenReturn(ProfileResponse.builder()
                        .id(1L)
                        .name(request.getName())
                        .build());

        // When
        ProfileResponse response = profileService.updateProfile(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(request.getName(), response.getName());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).findByUser(user);
        verify(profileRepository).save(argThat(p ->
                p.getName().equals(request.getName()) &&
                p.getBalance().equals(request.getBalance()) &&
                p.getMonthSalary().equals(request.getMonthSalary()) &&
                p.getCurrencyType().equals(request.getCurrencyType())
        ));
    }

    @Test
    void updateProfile_WhenProfileNotFound_ThenThrowsException() {
        // Given
        ProfileRequest request = new ProfileRequest();
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProfileNotFoundException.class, () -> profileService.updateProfile(request));

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).findByUser(user);
        verify(profileRepository, never()).save(any(Profile.class));
        verifyNoInteractions(profileMapper);
    }

    @Test
    void isProfileFilled_WhenProfileFilled_ThenReturnsTrue() {
        // Given
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.isProfileNeverUpdated(user.getId())).thenReturn(false);

        // When
        boolean response = profileService.isProfileFilled();

        // Then
        assertTrue(response);

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).isProfileNeverUpdated(user.getId());
    }

    @Test
    void isProfileFilled_WhenProfileNotFilled_ThenReturnsFalse() {
        // Given
        User user = new User();

        // Behavior stubs
        when(userService.getCurrentUser()).thenReturn(user);
        when(profileRepository.isProfileNeverUpdated(user.getId())).thenReturn(true);

        // When & Then
        assertThrows(ProfileNotFilledException.class, () -> profileService.isProfileFilled());

        // Verify
        verify(userService).getCurrentUser();
        verify(profileRepository).isProfileNeverUpdated(user.getId());
    }

    @Test
    void getCurrentUserProfile_WhenUserFound_ThenReturnsCurrentUserProfile() {
        // Given
        User user = new User();
        Profile profile = new Profile();

        // Behavior stubs
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        // When
        Profile profileResponse = profileService.getCurrentUserProfile(user);

        // Then
        assertNotNull(profileResponse);
        assertSame(profile, profileResponse);

        // Verify
        verify(profileRepository).findByUser(user);
    }

    @Test
    void getCurrentUserProfile_WhenUserNotFound_ThenThrowsException() {
        // Given
        User user = new User();

        // Behavior stubs
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProfileNotFoundException.class, () -> profileService.getCurrentUserProfile(user));

        // Verify
        verify(profileRepository).findByUser(user);
    }
}