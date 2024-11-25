package com.devaxiom.pos.auth;

import com.devaxiom.pos.email.OtpEmailService;
import com.devaxiom.pos.enums.Role;
import com.devaxiom.pos.exceptions.*;
import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.repositories.UsersRepository;
import com.devaxiom.pos.security.JwtService;
import com.devaxiom.pos.services.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UsersService usersService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UsersRepository usersRepository;
    private final OtpEmailService otpEmailService;

    public LoginResponseDto authenticateUser(LoginRequestDto loginRequest) {
        String email = loginRequest.getEmail();
        Optional<Users> userOptional = usersService.findUserOptionalByEmail(email);

        if (userOptional.isEmpty())
            throw new ResourceNotFoundException("User not found with email: " + email);

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            if (authentication.isAuthenticated()) {
                Users user = userOptional.get();
                if (!user.isVerified()) {
                    log.warn("User with email {} is not verified", user.getEmail());
                    throw new AccountNotVerifiedException("Account is not verified.");
                }
                String jwtToken = jwtService.generateJwtToken(user);
                log.info("User Role: {}", user.getRole().toString());
                return LoginResponseDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .jwtToken(jwtToken)
                        .role(user.getRole().name())
                        .isVerified(user.isVerified())
                        .build();
            }
        } catch (UnauthorizedException e) {
            throw new AccountNotVerifiedException("Account is Not Verified");
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid email or password");
        }
        throw new UnauthorizedException("Login failed");
    }

    public LoginResponseDto registerUser(@Valid RegistrationRequestDto registrationReq) {
        if (registrationReq == null)
            throw new BadRequestException("Registration request cannot be null.");

        if (usersRepository.existsByEmail(registrationReq.getEmail()))
            throw new ConflictException("Email already registered.");

        String role = registrationReq.getRole();
        log.info("Matched Role: {}", role);
        Users user = new Users();

        user.setEmail(registrationReq.getEmail());
        sendOTP(user);
        user.setPassword(passwordEncoder.encode(registrationReq.getPassword()));
        user.setRole(Role.valueOf(role));
        Users savedUser = usersRepository.save(user);
        return LoginResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(role)
                .build();
    }

    public void sendOTP(Users user) {
        log.info("Sending OTP to logged-in user with email: {}", user.getEmail());
        if (user.getEmail() == null) {
            log.error("Email cannot be null");
            throw new BadRequestException("Email cannot be null");
        }

        if (user.isVerified()) {
            log.warn("Account already verified for email: {}", user.getEmail());
            throw new BadRequestException("Account is already verified.");
        }

        String otp = otpEmailService.generateOtp();
        user.setOtp(Long.parseLong(otp));
        usersRepository.save(user);

        log.info("OTP {} sent to email: {}", otp, user.getEmail());
        otpEmailService.sendOtpEmail(user.getEmail(), otp);
    }

    public LoginResponseDto verifyOTP(String email, Long otp) {
        log.info("Verifying OTP for email: {}", email);
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> {
            log.error("User not found with email: {}", email);
            return new ResourceNotFoundException("User not found with email: " + email);
        });
        validateUserVerified(user);

        if (user.getOtp() != null && user.getOtp().equals(otp)) {
            user.setVerified(true);
            user.setOtp(null);
            usersRepository.save(user);

            String jwtToken = jwtService.generateJwtToken(user);

            log.info("OTP verified and user authenticated for email: {}", email);
            return LoginResponseDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .jwtToken(jwtToken)
                    .role(user.getRole().name())
                    .isVerified(user.isVerified())
                    .build();

        } else {
            log.error("Invalid OTP for email: {}", email);
            throw new BadRequestException("Invalid OTP");
        }
    }

    public void validateUserVerified(Users user) {
        if (user.isVerified()) {
            log.warn("User with email {} is already verified", user.getEmail());
            throw new BadRequestException("Account is already verified.");
        }
    }

    public void forgotPassword(String email) {
        log.info("Processing forgot password request for email: {}", email);
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> {
            log.error("User not found with email: {}", email);
            return new BadRequestException("User not found with email: " + email);
        });
        String otp = otpEmailService.generateOtp();
        user.setOtp(Long.parseLong(otp));
        usersRepository.save(user);
        otpEmailService.sendPasswordResetOtp(user.getEmail(), otp);
        log.info("Password reset OTP sent to email: {}", email);
    }

    public void resetPassword(String email, Long otp, String newPassword) {
        log.info("Processing password reset for email: {}", email);
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> {
            log.error("User not found with email: {}", email);
            return new BadRequestException("User not found with email: " + email);
        });

        if (!otp.equals(user.getOtp())) {
            log.error("Invalid OTP for email: {}", email);
            throw new BadRequestException("Invalid OTP for password reset.");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setOtp(null);
        usersRepository.save(user);
        log.info("Password reset successfully for user with email: {}", email);
    }
}

