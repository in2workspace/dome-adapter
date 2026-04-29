package es.altia.domeadapter.shared.domain.exception;

public class JWTParsingException extends RuntimeException {
    public JWTParsingException(String message) {
        super(message);
    }
}
