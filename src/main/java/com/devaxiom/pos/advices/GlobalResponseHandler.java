package com.devaxiom.pos.advices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // If the body is already an ApiResponse, ResponseEntity, String, or byte array, don’t wrap it
        if (body instanceof ApiResponse<?> || body instanceof ResponseEntity<?> || body instanceof byte[] || body instanceof String)
            return body;

//         Avoid wrapping documentation endpoints and static resources
        String requestURI = request.getURI().getPath();
        log.info("Request URI: {}", requestURI);
        if (requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/get-image") ||
                requestURI.startsWith("/ws/**") ||
                requestURI.startsWith("/chat") ||
                requestURI.contains("/ws")
        ) {
            return body;
        }

        // Otherwise, wrap the response body in ApiResponse
        return new ApiResponse<>(body, "Operation successful");
    }
}
