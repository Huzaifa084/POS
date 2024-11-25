package com.devaxiom.pos.auth;


import com.devaxiom.pos.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateProfileDto {
    @NotBlank(message = "Name is required")
    private String name;
    @NotNull(message = "Gender is required")
    private Gender gender;
    private String dateOfBirth;
    private String phoneNumber;
    private String country;
    private String city;
    private boolean available24_7;
    private String landlineNumber;
    private String location;
    private String emiratesId;
    private String topSkills;
    private String language;
    private Double consultationPrice;
    private String biography;
    private String license;
    private Double translationPrice;
    private String certifications;
    private String profilePicture;
    private String emiratesIdFront;
    private String emiratesIdBack;
}