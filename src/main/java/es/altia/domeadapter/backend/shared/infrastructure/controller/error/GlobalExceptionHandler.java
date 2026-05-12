package es.altia.domeadapter.backend.shared.infrastructure.controller.error;

import es.altia.domeadapter.backend.shared.domain.exception.FormatUnsupportedException;
import es.altia.domeadapter.backend.shared.domain.exception.JWTParsingException;
import es.altia.domeadapter.backend.shared.domain.exception.JWTVerificationException;
import es.altia.domeadapter.backend.shared.domain.exception.ProofValidationException;
import es.altia.domeadapter.backend.shared.domain.model.dto.GlobalErrorMessage;
import es.altia.domeadapter.backend.shared.domain.util.GlobalErrorTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import javax.naming.OperationNotSupportedException;
import java.text.ParseException;
import java.util.NoSuchElementException;

//todo make recursive
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseFactory errors;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<GlobalErrorMessage> handleNoSuchElementException(
            NoSuchElementException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.NO_SUCH_ELEMENT.getCode(),
                "Resource not found",
                HttpStatus.NOT_FOUND,
                "The requested resource was not found"
        );
    }

    @ExceptionHandler(ParseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<GlobalErrorMessage> handleParseException(
            ParseException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.PARSE_ERROR.getCode(),
                "Parse error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal parsing error occurred."
        );
    }

    @ExceptionHandler(ProofValidationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<GlobalErrorMessage> handleProofValidationException(
            ProofValidationException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.PROOF_VALIDATION_ERROR.getCode(),
                "Proof validation error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal proof validation error occurred."
        );
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<GlobalErrorMessage> handleOperationNotSupportedException(
            OperationNotSupportedException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.OPERATION_NOT_SUPPORTED.getCode(),
                "Operation not supported",
                HttpStatus.BAD_REQUEST,
                "The given operation is not supported"
        );
    }

    @ExceptionHandler(JWTVerificationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<GlobalErrorMessage> handleJWTVerificationException(
            JWTVerificationException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.JWT_VERIFICATION.getCode(),
                "JWT verification failed",
                HttpStatus.UNAUTHORIZED,
                "JWT verification failed."
        );
    }


    @ExceptionHandler(JWTParsingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<GlobalErrorMessage> handleJWTParsingException(
            JWTParsingException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.INVALID_JWT.getCode(),
                "JWT parsing error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The provided JWT is invalid or can't be parsed."
        );
    }

    @ExceptionHandler(FormatUnsupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<GlobalErrorMessage> handleFormatUnsupportedException(
            FormatUnsupportedException ex,
            ServerHttpRequest request
    ) {
        return errors.handleWith(
                ex, request,
                GlobalErrorTypes.FORMAT_IS_NOT_SUPPORTED.getCode(),
                "Format not supported",
                HttpStatus.BAD_REQUEST,
                "Format is not supported"
        );
    }


}

