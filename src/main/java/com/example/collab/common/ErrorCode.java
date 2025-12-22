package com.example.collab.common;

public enum ErrorCode {
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    VALIDATION_ERROR(400, "Validation Error"),
    BUSINESS_ERROR(422, "Business Error"),
    SERVER_ERROR(500, "Server Error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
