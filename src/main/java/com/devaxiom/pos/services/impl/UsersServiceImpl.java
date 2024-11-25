package com.devaxiom.pos.services.impl;

import com.devaxiom.briefcase.dto.ClientProfileResponseDto;
import com.devaxiom.briefcase.dto.CreateProfileDto;
import com.devaxiom.briefcase.dto.LawyerProfileResponseDto;
import com.devaxiom.briefcase.dto.TranslationCompanyProfileResponseDto;
import com.devaxiom.briefcase.enums.Role;
import com.devaxiom.briefcase.exceptions.AccessDeniedException;
import com.devaxiom.briefcase.exceptions.ResourceNotFoundException;
import com.devaxiom.briefcase.exceptions.UserException;
import com.devaxiom.briefcase.exceptions.UsernameNotFoundException;
import com.devaxiom.briefcase.model.Client;
import com.devaxiom.briefcase.model.Lawyer;
import com.devaxiom.briefcase.model.TranslationCompany;
import com.devaxiom.briefcase.model.Users;
import com.devaxiom.briefcase.repositories.ClientRepository;
import com.devaxiom.briefcase.repositories.LawyerRepository;
import com.devaxiom.briefcase.repositories.TranslationCompanyRepository;
import com.devaxiom.briefcase.repositories.UsersRepository;
import com.devaxiom.briefcase.security.JwtService;
import com.devaxiom.briefcase.services.UsersService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final ClientRepository clientRepository;
    private final LawyerRepository lawyerRepository;
    private final TranslationCompanyRepository translationCompanyRepository;

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

    public Object getUserProfile(Users user) {
        if (user.getRole() == Role.client) {
            Client client = clientRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Client not found with email: " + user.getEmail()));
            ClientProfileResponseDto responseDto = modelMapper.map(user, ClientProfileResponseDto.class);
            modelMapper.map(client, responseDto);
            return responseDto;

        } else if (user.getRole() == Role.lawPractitioner) {
            Lawyer lawyer = lawyerRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Lawyer not found with email: " + user.getEmail()));
            LawyerProfileResponseDto responseDto = modelMapper.map(user, LawyerProfileResponseDto.class);
            modelMapper.map(lawyer, responseDto);
            return responseDto;

        } else if (user.getRole() == Role.translationCompany) {
            TranslationCompany translationCompany = translationCompanyRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Translation Company not found with email: " + user.getEmail()));
            TranslationCompanyProfileResponseDto responseDto = modelMapper.map(user, TranslationCompanyProfileResponseDto.class);
            modelMapper.map(translationCompany, responseDto);
            return responseDto;
        }

        throw new IllegalArgumentException("Unsupported role: " + user.getRole());
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

    @Override
    public Object createProfile(Users user, CreateProfileDto profileDto) throws UserException {
        Long userId = user.getId();

        return switch (user.getRole()) {
            case client -> updateClient(userId, profileDto);
            case lawPractitioner -> updateLawPractitioner(userId, profileDto);
            case translationCompany -> updateTranslationCompany(userId, profileDto);
            default -> throw new UserException("Unsupported role: " + user.getRole());
        };
    }

    private ClientProfileResponseDto updateClient(Long id, CreateProfileDto profileDto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + id));

        // Map fields manually
        client.setName(profileDto.getName());
        client.setGender(profileDto.getGender());
        client.setDateOfBirth(profileDto.getDateOfBirth() != null ? LocalDate.parse(profileDto.getDateOfBirth()) : null);
        client.setPhoneNumber(profileDto.getPhoneNumber());
        client.setCountry(profileDto.getCountry());
        client.setCity(profileDto.getCity());
        client.setAvailable24_7(profileDto.isAvailable24_7());
        client.setEmiratesId(profileDto.getEmiratesId());
        client.setEmiratesIdFront(profileDto.getEmiratesIdFront());
        client.setEmiratesIdBack(profileDto.getEmiratesIdBack());

        // Save and map to DTO
        clientRepository.save(client);
        return ClientProfileResponseDto.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .gender(client.getGender())
                .dateOfBirth(client.getDateOfBirth())
                .phoneNumber(client.getPhoneNumber())
                .country(client.getCountry())
                .city(client.getCity())
                .available24_7(client.isAvailable24_7())
                .emiratesId(client.getEmiratesId())
                .emiratesIdFront(client.getEmiratesIdFront())
                .emiratesIdBack(client.getEmiratesIdBack())
                .role(client.getRole().name())
                .build();
    }

    private LawyerProfileResponseDto updateLawPractitioner(Long id, CreateProfileDto profileDto) {
        Lawyer lawyer = lawyerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with ID: " + id));

        // Map fields manually
        lawyer.setName(profileDto.getName());
        lawyer.setGender(profileDto.getGender());
        lawyer.setDateOfBirth(profileDto.getDateOfBirth() != null ? LocalDate.parse(profileDto.getDateOfBirth()) : null);
        lawyer.setPhoneNumber(profileDto.getPhoneNumber());
        lawyer.setCountry(profileDto.getCountry());
        lawyer.setCity(profileDto.getCity());
        lawyer.setAvailable24_7(profileDto.isAvailable24_7());
        lawyer.setLandlineNumber(profileDto.getLandlineNumber());
        lawyer.setLocation(profileDto.getLocation());
        lawyer.setEmiratesId(profileDto.getEmiratesId());
        lawyer.setTopSkills(profileDto.getTopSkills());
        lawyer.setLanguage(profileDto.getLanguage());
        lawyer.setConsultationPrice(profileDto.getConsultationPrice());
        lawyer.setBiography(profileDto.getBiography());
        lawyer.setLicense(profileDto.getLicense());
        lawyer.setCertifications(profileDto.getCertifications());
        lawyer.setEmiratesIdFront(profileDto.getEmiratesIdFront());
        lawyer.setEmiratesIdBack(profileDto.getEmiratesIdBack());

        // Save and map to DTO
        lawyerRepository.save(lawyer);
        return LawyerProfileResponseDto.builder()
                .id(lawyer.getId())
                .name(lawyer.getName())
                .email(lawyer.getEmail())
                .gender(lawyer.getGender())
                .dateOfBirth(lawyer.getDateOfBirth())
                .phoneNumber(lawyer.getPhoneNumber())
                .country(lawyer.getCountry())
                .city(lawyer.getCity())
                .available24_7(lawyer.isAvailable24_7())
                .landlineNumber(lawyer.getLandlineNumber())
                .location(lawyer.getLocation())
                .emiratesId(lawyer.getEmiratesId())
                .topSkills(lawyer.getTopSkills())
                .language(lawyer.getLanguage())
                .consultationPrice(lawyer.getConsultationPrice())
                .biography(lawyer.getBiography())
                .license(lawyer.getLicense())
                .certifications(lawyer.getCertifications())
                .emiratesIdFront(lawyer.getEmiratesIdFront())
                .emiratesIdBack(lawyer.getEmiratesIdBack())
                .role(lawyer.getRole().name())
                .build();
    }

    private TranslationCompanyProfileResponseDto updateTranslationCompany(Long id, CreateProfileDto profileDto) {
        TranslationCompany company = translationCompanyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translation Company not found with ID: " + id));

        // Map fields manually
        company.setName(profileDto.getName());
        company.setGender(profileDto.getGender());
        company.setDateOfBirth(profileDto.getDateOfBirth() != null ? LocalDate.parse(profileDto.getDateOfBirth()) : null);
        company.setPhoneNumber(profileDto.getPhoneNumber());
        company.setCountry(profileDto.getCountry());
        company.setCity(profileDto.getCity());
        company.setAvailable24_7(profileDto.isAvailable24_7());
        company.setLandlineNumber(profileDto.getLandlineNumber());
        company.setLocation(profileDto.getLocation());
        company.setLanguage(profileDto.getLanguage());
        company.setTranslationPrice(profileDto.getTranslationPrice());
        company.setBiography(profileDto.getBiography());
        company.setLicense(profileDto.getLicense());
        company.setCertifications(profileDto.getCertifications());

        // Save and map to DTO
        translationCompanyRepository.save(company);
        return TranslationCompanyProfileResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .gender(company.getGender())
                .dateOfBirth(company.getDateOfBirth())
                .phoneNumber(company.getPhoneNumber())
                .country(company.getCountry())
                .city(company.getCity())
                .available24_7(company.isAvailable24_7())
                .landlineNumber(company.getLandlineNumber())
                .location(company.getLocation())
                .language(company.getLanguage())
                .translationPrice(company.getTranslationPrice())
                .biography(company.getBiography())
                .license(company.getLicense())
                .certifications(company.getCertifications())
                .role(company.getRole().name())
                .build();
    }

}