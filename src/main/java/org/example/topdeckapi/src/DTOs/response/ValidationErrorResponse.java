package org.example.topdeckapi.src.DTOs.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
        Integer status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {}
