package com.devaxiom.pos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor

@Getter
@Setter
public class LoginRequestDto {

    @Schema(description = "User's email address for login", example = "huzaifanaseer074@gmail.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's password for login", example = "1234", required = true)
    @NotBlank(message = "Password is required")
    private String password;
}