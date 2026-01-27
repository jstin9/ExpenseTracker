package com.jstn9.expensetracker.service;

import com.jstn9.expensetracker.dto.auth.RegistrationRequest;
import com.jstn9.expensetracker.dto.auth.UserResponse;
import com.jstn9.expensetracker.exception.EmailAlreadyExistsException;
import com.jstn9.expensetracker.exception.RoleNotFoundException;
import com.jstn9.expensetracker.exception.UsernameAlreadyExistsException;
import com.jstn9.expensetracker.exception.UsernameNotFoundException;
import com.jstn9.expensetracker.mapper.UserMapper;
import com.jstn9.expensetracker.model.Role;
import com.jstn9.expensetracker.model.User;
import com.jstn9.expensetracker.model.enums.RoleNames;
import com.jstn9.expensetracker.repository.RoleRepository;
import com.jstn9.expensetracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ProfileService profileService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper, ProfileService profileService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.profileService = profileService;
    }

    @Transactional
    public UserResponse save(RegistrationRequest request) {

        isUserExist(request);

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName(RoleNames.ROLE_USER).orElseThrow(RoleNotFoundException::new);
        newUser.getRoles().add(userRole);

        User savedUser = userRepository.save(newUser);

        profileService.createEmptyProfile(savedUser);

        return userMapper.toUserResponse(savedUser);
    }

    public User getCurrentUser(){
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(UsernameNotFoundException::new);
    }

    private void isUserExist(RegistrationRequest user){
        if (userRepository.existsUserByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        if (userRepository.existsUserByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException();
        }
    }
}
