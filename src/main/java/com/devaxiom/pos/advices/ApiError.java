package com.devaxiom.pos.advices;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
@Builder
public class ApiError {
    private HttpStatus errorStatus;
    private String message;
    private List<String> subErrors;
}