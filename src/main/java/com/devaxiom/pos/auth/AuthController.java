package com.devaxiom.pos.auth;


import com.devaxiom.pos.advices.ApiResponse;
import com.devaxiom.pos.exceptions.UnauthorizedException;
import com.devaxiom.pos.model.Users;
import com.devaxiom.pos.repositories.UsersRepository;
import com.devaxiom.pos.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UsersRepository usersRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody @Valid LoginRequestDto loginRequest) {
        log.info("Attempting login for email: {}", loginRequest.getEmail());

        LoginResponseDto loginResponseDto = authService.authenticateUser(loginRequest);
        ApiResponse<LoginResponseDto> response = new ApiResponse<>(loginResponseDto, "Login successful");
        return ResponseEntity.ok(response);

    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponseDto>> register(@RequestBody @Valid RegistrationRequestDto registrationReq) {
        log.info("Attempting register for email: {}", registrationReq.getEmail());
        try {
            LoginResponseDto loginResponseDto = authService.registerUser(registrationReq);
            ApiResponse<LoginResponseDto> response = new ApiResponse<>(loginResponseDto, "Registration successful");
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            log.error("Registration failed for email: {}", registrationReq.getEmail());
            ApiResponse<LoginResponseDto> response = new ApiResponse<>(null, "Registration failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @Operation(summary = "To Re Send OTP to user email")
    @PostMapping("/opt/resend")
    public ResponseEntity<String> sendOTP(@RequestParam String email) {
        log.info("Sending OTP to email: {}", email);
        Users users = usersRepository.findByEmail(email).orElseThrow(
                () -> new UnauthorizedException("User not found with email: " + email)
        );
        authService.sendOTP(users);
        return ResponseEntity.ok("OTP has been sent to your email.");
    }

    @Operation(summary = "Verify OTP")
    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponseDto> verifyOTP(@RequestBody @Valid OtpDto otpDto) {
        log.info("Verifying OTP for user: {}", otpDto.getEmail());
        LoginResponseDto authResponse = authService.verifyOTP(otpDto.getEmail(), otpDto.getOtp());

        if (authResponse != null) {
            log.info("OTP verified successfully for user: {}. Sending AuthResponse.", otpDto.getEmail());
            return ResponseEntity.ok(authResponse);
        } else {
            log.warn("Invalid OTP for user: {}", otpDto.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Logout user")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring(7);
            tokenBlacklistService.blacklistToken(jwtToken);

            log.info("User logged out successfully. Token blacklisted.");
            return ResponseEntity.ok("User logged out successfully");
        }

        log.warn("Logout attempt failed. No token found in the request.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No token found");
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Forgot password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        log.info("Forgot password request for email: {}", email);
        authService.forgotPassword(email);
        return ResponseEntity.ok("Password reset instructions have been sent to your email.");
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password")
    public ResponseEntity<String> resetPassword(@RequestParam String email, @RequestParam Long otp, @RequestParam String newPassword) {
        log.info("Password reset request with email: {}", email);
        authService.resetPassword(email, otp, newPassword);
        return ResponseEntity.ok("Password has been reset successfully.");
    }

}
