package es.altia.domeadapter.shared.domain.exception;

public class JWTClaimMissingException extends RuntimeException {
    public JWTClaimMissingException(String message) {
        super(message);
    }
}
