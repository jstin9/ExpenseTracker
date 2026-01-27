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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final ProfileMapper profileMapper;

    public ProfileService(ProfileRepository profileRepository, UserService userService, ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.userService = userService;
        this.profileMapper = profileMapper;
    }

    public ProfileResponse getProfile() {
        User user = userService.getCurrentUser();
        Profile profile = getCurrentUserProfile(user);
        return profileMapper.toProfileResponse(profile);
    }

    public void createEmptyProfile(User user){
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setName("");
        profile.setBalance(BigDecimal.ZERO);
        profile.setMonthSalary(BigDecimal.ZERO);
        profile.setCurrencyType(CurrencyType.EUR);

        profileRepository.save(profile);
    }

    public void saveProfile(Profile profile){
        profileRepository.save(profile);
    }

    public ProfileResponse updateProfile(ProfileRequest request) {
        User user = userService.getCurrentUser();
        Profile profile = getCurrentUserProfile(user);

        profile.setName(request.getName());
        profile.setBalance(request.getBalance());
        profile.setMonthSalary(request.getMonthSalary());
        profile.setCurrencyType(request.getCurrencyType());
        Profile savedProfile = profileRepository.save(profile);

        return profileMapper.toProfileResponse(savedProfile);
    }

    public boolean isProfileFilled(){
        User user = userService.getCurrentUser();
        boolean neverUpdated = profileRepository.isProfileNeverUpdated(user.getId());
        if(neverUpdated){
            throw new ProfileNotFilledException();
        }
        return true;
    }

    public Profile getCurrentUserProfile(User user){
        return profileRepository.findByUser(user)
                .orElseThrow(ProfileNotFoundException::new);
    }
}
