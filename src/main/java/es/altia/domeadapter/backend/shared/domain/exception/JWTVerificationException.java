package es.altia.domeadapter.shared.domain.exception;

public class JWTVerificationException extends RuntimeException {
    public JWTVerificationException(String message) {
        super(message);
    }
}
