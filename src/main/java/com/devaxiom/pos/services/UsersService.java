package com.devaxiom.pos.services;


import com.devaxiom.pos.auth.CreateProfileDto;
import com.devaxiom.pos.exceptions.UserException;
import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.model.Users;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public interface UsersService {

    Users findUserByEmail(String email);

    Users findUserById(Long id) throws UserException;

    Optional<Users> findUserByName(String userName);

    Users findUserProfile(String jwt) throws UserException;

    Object createProfile(Users user, CreateProfileDto updateUserRequestDto) throws UserException;

    List<Users> searchUser(String query);

    Optional<Users> findUserOptionalByEmail(String email);

    Object getUserProfile(Users user);

    String uploadProfilePic(MultipartFile file, String uploadDir) throws IOException;
}
