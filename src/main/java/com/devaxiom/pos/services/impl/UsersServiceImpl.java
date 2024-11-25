package com.devaxiom.pos.services.impl;

import com.devaxiom.pos.exceptions.AccessDeniedException;
import com.devaxiom.pos.exceptions.UserException;
import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.repositories.UsersRepository;
import com.devaxiom.pos.security.JwtService;
import com.devaxiom.pos.services.UsersService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;

    @Override
    public Users findUserByEmail(String email) {
        Optional<Users> user = usersRepository.findByEmail(email);
        return user.orElse(null);
    }

    public Optional<Users> findUserOptionalByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    @Override
    public Users findUserById(Long id) throws UserException {
        Optional<Users> user = usersRepository.findById(id);
        if (user.isPresent()) return user.get();
        throw new UserException("User not found with id: " + id);
    }

    @Override
    public Optional<Users> findUserByName(String userName) {
        return usersRepository.findByName(userName);
    }

    @Override
    public Users findUserProfile(String jwt) throws UserException {
        String email = jwtService.extractUserNameFromJwt(jwt);
        if (email == null) throw new AccessDeniedException("Invalid JWT");
        Users user = this.findUserByEmail(email);
        if (user == null) throw new UserException("User not found with email: " + email);
        return user;
    }

    public List<Users> searchUser(String query) {
        return usersRepository.searchUser(query);
    }


    @Override
    public String uploadProfilePic(MultipartFile file, String uploadDir) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        String contentType = file.getContentType();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new IllegalArgumentException("Only JPEG or PNG images are allowed");
        }
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

}