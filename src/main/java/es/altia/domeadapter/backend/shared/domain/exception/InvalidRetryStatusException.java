package es.altia.domeadapter.backend.shared.domain.exception;

public class InvalidRetryStatusException extends RuntimeException {
    public InvalidRetryStatusException(String message) {
        super(message);
    }
}
