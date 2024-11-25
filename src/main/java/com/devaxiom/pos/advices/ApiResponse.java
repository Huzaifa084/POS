package com.devaxiom.pos.advices;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private ApiError error;
    private boolean status;
    private String message;
    private T data;


    public ApiResponse(T data, String message) {
        this.data = data;
        this.status = true;
        this.message = message;
    }

    public ApiResponse(ApiError error, String message) {
        this.error = error;
        this.status = false;
        this.message = message;
    }

    public ApiResponse(T data) {
        this(data, "Operation successful");
    }

    public ApiResponse(ApiError error) {
        this(error, "Operation failed");
    }

    public ApiResponse() {
        this.status = true;
        this.message = "Operation successful";
    }
}
