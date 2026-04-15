package org.example.topdeckapi.src.DTOs.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        Integer status,
        String error,
        String message,
        String path,
        LocalDateTime timeStamp
) {}
