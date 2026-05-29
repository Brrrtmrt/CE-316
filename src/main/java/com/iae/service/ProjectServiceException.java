package com.iae.service;

public class ProjectServiceException extends RuntimeException {

    public ProjectServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectServiceException(String message) {
        super(message);
    }
}
