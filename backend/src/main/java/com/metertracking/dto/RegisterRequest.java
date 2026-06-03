package com.metertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private String username;
    private String role;
    //  НОВОЕ ПОЛЕ: регион пользователя
    private String region;
}