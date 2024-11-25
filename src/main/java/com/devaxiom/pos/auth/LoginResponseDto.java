package com.devaxiom.pos.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class LoginResponseDto {
    private Long id;
    private String email;
    private String name;
    private String jwtToken;
    private boolean isVerified;
    private String role;
}

