package com.devaxiom.pos.advices;

import com.devaxiom.pos.exceptions.*;
import com.devaxiom.pos.exceptions.IllegalArgumentException;
import com.devaxiom.pos.exceptions.ResourceNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .subErrors(List.of("The requested resource could not be found."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(HttpServerErrorException.InternalServerError.class)
    public ResponseEntity<Object> handleInternalServerError(HttpServletRequest request, Exception exception) {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/v3/api-docs") || requestURI.startsWith("/swagger-ui"))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());

        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(exception.getMessage())
                .subErrors(List.of("An unexpected error occurred."))
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getErrorStatus());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleInputValidationErrors(MethodArgumentNotValidException exception) {
        List<String> errors = exception
                .getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message("Input validation failed")
                .subErrors(errors)
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(ConflictException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.CONFLICT)
                .message(exception.getMessage())
                .subErrors(List.of("There was a conflict with an existing resource."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.FORBIDDEN)
                .message(exception.getMessage())
                .subErrors(List.of("You do not have permission to access this resource."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message(exception.getMessage())
                .subErrors(List.of("Invalid request data."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.UNAUTHORIZED)
                .message(exception.getMessage())
                .subErrors(List.of("Authentication failed due to invalid credentials."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiResponse<?>> handleEmailException(EmailException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Email sending failed")
                .subErrors(List.of(exception.getMessage()))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(KeyGenerationException.class)
    public ResponseEntity<ApiResponse<?>> handleKeyGenerationException(KeyGenerationException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Key generation failed")
                .subErrors(List.of(exception.getMessage()))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("An unexpected error occurred: " + ex.getMessage())
                .subErrors(List.of("Check server logs for more details"))
                .build();

        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getErrorStatus());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingRequestParameter(MissingServletRequestParameterException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message(exception.getParameterName() + " parameter is missing")
                .subErrors(List.of("Missing required '" + exception.getParameterName() + "' parameter"))
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUsernameNotFound(UsernameNotFoundException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .subErrors(List.of("The specified username was not found in the system."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.FORBIDDEN)
                .message(exception.getMessage())
                .subErrors(List.of("Access is forbidden due to authentication failure."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<?>> handleSignatureException(SignatureException exception) {
        log.error("JWT signature verification failed: {}", exception.getMessage());
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.UNAUTHORIZED)
                .message("JWT signature does not match. The token is invalid.")
                .subErrors(List.of("Invalid JWT signature."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<?>> handleExpiredJwtException(ExpiredJwtException exception) {
        log.warn("Attempted to use an expired JWT: {}", exception.getMessage());
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.UNAUTHORIZED)
                .message("JWT has expired. Please login again to obtain a new token.")
                .subErrors(List.of("Expired JWT token."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtException(JwtException exception) {
        log.error("JWT processing failed: {}", exception.getMessage());
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.UNAUTHORIZED)
                .message("Error processing JWT. Please check the token and try again.")
                .subErrors(List.of("JWT processing error."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<?>> handleDuplicateResourceException(DuplicateResourceException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.CONFLICT)
                .message(exception.getMessage())
                .subErrors(List.of("There was a conflict with an existing resource."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("Invalid argument provided: {}", exception.getMessage());
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message("Invalid argument: " + exception.getMessage())
                .subErrors(List.of("Check the provided input parameters."))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<?>> handleUserException(UserException e, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message(e.getMessage() + "\n" + request.getDescription(false))
                .subErrors(List.of("Invalid user data.", request.getDescription(false)))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<?>> handleChatException(ChatException exception, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.BAD_REQUEST)
                .message(exception.getMessage())
                .subErrors(List.of("A chat-related error occurred.", request.getDescription(false)))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<?>> handleIOException(IOException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("I/O error occurred: " + exception.getMessage())
                .subErrors(List.of("I/O error"))
                .build();
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccountNotVerifiedException(AccountNotVerifiedException exception) {
        ApiError apiError = ApiError.builder()
                .errorStatus(HttpStatus.FORBIDDEN)
                .message(exception.getMessage())
                .subErrors(List.of("Account verification is required to perform this action."))
                .build();
        return buildErrorResponseEntity(apiError);
    }



    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError) {
        ApiResponse<?> apiResponse = new ApiResponse<>(apiError, "Operation failed");
        return new ResponseEntity<>(apiResponse, apiError.getErrorStatus());
    }

}
