package org.example.topdeckapi.src.DTOs.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        Integer status,
        String error,
        String message,
        String path,
        LocalDateTime timeStamp,
        String code
) {
    public ApiErrorResponse(Integer status, String error, String message, String path, LocalDateTime timeStamp) {
        this(status, error, message, path, timeStamp, null);
    }
}
