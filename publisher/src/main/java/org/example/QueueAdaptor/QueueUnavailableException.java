package org.example.QueueAdaptor;

public class QueueUnavailableException extends RuntimeException {

    public QueueUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
