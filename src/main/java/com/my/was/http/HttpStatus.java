package com.my.was.http;

public enum HttpStatus {
    OK(200, "OK"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int statusCode;
    private final String statusText;

    HttpStatus(int statusCode, String statusText) {
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public static HttpStatus fromStatusCode(int statusCode) {
        for (HttpStatus status : values()) {
            if (status.statusCode == statusCode) {
                return status;
            }
        }
        return null; // 또는 throw new IllegalArgumentException("Invalid status code: " + statusCode);
    }
}