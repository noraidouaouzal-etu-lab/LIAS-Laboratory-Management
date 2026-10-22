package com.lias.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    private String fullName;
    private String email;
    private String username;
    private String password;
    private String confirmPassword;
    private String status; // Doctorant / Member / Associate
}
