package es.altia.domeadapter.backend.shared.domain.exception;

public class ProcedureRetryRecordNotFoundException extends RuntimeException {
    public ProcedureRetryRecordNotFoundException(String message) {
        super(message);
    }
}
