package com.devaxiom.pos.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpDto {
    @NotNull(message = "OTP cannot be null")
    private Long otp;

    @NotNull(message = "Email cannot be null")
    private String email;
}
