package com.xz.hello.expection;

public class BusinessException extends Exception {
    private String message;
    private Throwable exception;

    public BusinessException() {

    }

    public BusinessException(String message, Throwable exception) {
        this.message = message;
        this.exception = exception;
    }
}
