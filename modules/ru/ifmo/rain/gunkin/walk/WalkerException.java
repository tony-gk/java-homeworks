package ru.ifmo.rain.gunkin.walk;

public class WalkerException extends Exception {
    public WalkerException(String errorMessage) {
        super(errorMessage);
    }

    public WalkerException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
