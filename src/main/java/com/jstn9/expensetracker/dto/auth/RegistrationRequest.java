package com.jstn9.expensetracker.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationRequest {

    @NotBlank(message = "Username cannot be empty!")
    @Size(min = 3, max = 25, message = "Username must be 3–25 characters long!")
    private String username;

    @Email(message = "Email must be valid!")
    private String email;

    @NotBlank
    @Size(min = 7, message = "The password must be at least 7 characters long!")
    private String password;
}
