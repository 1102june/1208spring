package com.example.youth.exception;

public class FirebaseTokenException extends RuntimeException {
    public FirebaseTokenException(String message) {
        super(message);
    }

    public FirebaseTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

