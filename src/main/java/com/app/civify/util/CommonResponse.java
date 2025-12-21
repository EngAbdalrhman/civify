package com.app.civify.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.civify.exception.ErrorCode;

@Data
@NoArgsConstructor
public class CommonResponse<T> {

    private String status;
    private String message;
    private String errorCode;
    private T data;
    private LocalDateTime timestamp;
    private String requestId;

    public CommonResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public CommonResponse(boolean success, String message, T data) {
        this.status = success ? "success" : "error";
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public CommonResponse(T data) {
        this.status = "success";
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public CommonResponse(String message, boolean success) {
        this.message = message;
        this.status = success ? "success" : "error";
        this.timestamp = LocalDateTime.now();
    }

    public static <T> CommonResponse<T> success(String message, T data) {
        CommonResponse<T> response = new CommonResponse<>("success", message, data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> CommonResponse<T> success(T data) {
        CommonResponse<T> response = new CommonResponse<>("success", "Operation successful", data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> CommonResponse<T> error(String message) {
        CommonResponse<T> response = new CommonResponse<>("error", message, null);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    public static <T> CommonResponse<T> error(String message, T data) {
        CommonResponse<T> response = new CommonResponse<>("error", message, data);
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    /**
     * Create error response with error code
     */
    public static <T> CommonResponse<T> error(ErrorCode errorCode, String message) {
        CommonResponse<T> response = new CommonResponse<>("error", message, null);
        response.setErrorCode(errorCode.getCode());
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }

    /**
     * Create error response with error code and data
     */
    public static <T> CommonResponse<T> error(ErrorCode errorCode, String message, T data) {
        CommonResponse<T> response = new CommonResponse<>("error", message, data);
        response.setErrorCode(errorCode.getCode());
        response.setTimestamp(LocalDateTime.now());
        response.setRequestId(UUID.randomUUID().toString());
        return response;
    }
}
