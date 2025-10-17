package com.app.civify.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.status = success ? "success" : "error";
        this.message = message;
        this.data = data;
    }

    public ApiResponse(T data) {
        this.status = "success";
        this.data = data;
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.status = success ? "success" : "error";
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "Operation successful", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>("error", message, data);
    }
}
