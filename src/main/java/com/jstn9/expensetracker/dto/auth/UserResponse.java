package com.jstn9.expensetracker.dto.auth;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
}
