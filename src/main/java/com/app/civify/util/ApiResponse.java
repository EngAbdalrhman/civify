package com.app.civify.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String requestId;

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.status = success ? "success" : "error";
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(T data) {
        this.status = "success";
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.status = success ? "success" : "error";
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>("success", message, data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>("success", "Operation successful", data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>("error", message, null);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>("error", message, data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }
}
