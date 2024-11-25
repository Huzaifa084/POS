package com.devaxiom.pos.services.impl;

import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.repositories.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to load user by email: {}", email);
        Users users = usersRepository.findByEmail(email).orElseThrow(()
                -> new UsernameNotFoundException(email + " not found"));
        log.info("User Password: {}", users.getPassword());
        log.info("User Role: {}", users.getAuthorities());
        return new User(users.getEmail(), users.getPassword(), users.getAuthorities());
//        return users;
    }

    public UserDetails loadUserById(Long userId) {
        Users users = usersRepository.findById(userId).orElseThrow(()
                -> new UsernameNotFoundException(userId + " not found"));
        return new User(users.getEmail(), users.getPassword(), new ArrayList<>());
    }
}

