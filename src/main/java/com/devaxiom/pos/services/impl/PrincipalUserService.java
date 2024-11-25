package com.devaxiom.pos.services.impl;

import com.devaxiom.pos.enums.Role;
import com.devaxiom.pos.exceptions.ResourceNotFoundException;
import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrincipalUserService {
    private final UsersRepository userEntityRepository;

    public String getLoggedInUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public Users getLoggedInUser() {
        String email = this.getLoggedInUserEmail();
        return userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public Long getLoggedInUserId() {
        return this.getLoggedInUser().getId();
    }

    public String getLoggedInUserName() {
        return this.getLoggedInUser().getName();
    }

    public boolean isAdmin() {
        return this.getLoggedInUser().getRole() == Role.ADMIN;
    }
}
