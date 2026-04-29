package es.altia.domeadapter.shared.domain.exception;

public class RetryConfigurationException extends RuntimeException {
    public RetryConfigurationException(String message) {
        super(message);
    }
}
