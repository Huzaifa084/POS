package com.devaxiom.pos.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OtpEmailService {

    private final EmailService emailService;

    public OtpEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public String generateOtp() {
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        log.debug("Generated OTP: {}", otp);
        return otp;
    }

    public void sendOtpEmail(String email, String otp) {
        String subject = "OTP for account verification";
        String mailText = "Your OTP is: " + otp + ". Please use this to verify your account.";
        log.info("Sending OTP email to: {}", email);
        emailService.sendEmail(email, subject, mailText);
    }

    public void sendPasswordResetOtp(String email, String otp) {
        String subject = "Password Reset OTP";
        String message = "Your OTP for password reset is: " + otp + ". Please use this to reset your password.";
        log.info("Sending password reset OTP to: {}", email);
        emailService.sendEmail(email, subject, message);
    }
}

