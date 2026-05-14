package es.altia.domeadapter.backend.issuance.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.altia.domeadapter.backend.issuance.domain.service.IssuerCoreClientPort;
import es.altia.domeadapter.backend.shared.domain.exception.FormatUnsupportedException;
import es.altia.domeadapter.backend.shared.domain.exception.InvalidCredentialFormatException;
import es.altia.domeadapter.backend.shared.domain.exception.MissingEmailOwnerException;
import es.altia.domeadapter.backend.shared.domain.exception.MissingIdTokenHeaderException;
import es.altia.domeadapter.backend.shared.domain.exception.UnsupportedCredentialSchemaException;
import es.altia.domeadapter.backend.shared.domain.model.dto.ExternalPreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.credential.LabelCredential;
import es.altia.domeadapter.backend.shared.domain.model.dto.credential.lear.employee.LEARCredentialEmployee;
import es.altia.domeadapter.backend.shared.domain.model.dto.credential.lear.machine.LEARCredentialMachine;
import es.altia.domeadapter.backend.shared.domain.model.dto.retry.LabelCredentialDeliveryPayload;
import es.altia.domeadapter.backend.shared.domain.model.enums.ActionType;
import es.altia.domeadapter.backend.shared.domain.service.M2MTokenService;
import es.altia.domeadapter.backend.shared.domain.service.ProcedureRetryService;
import es.altia.domeadapter.backend.shared.domain.util.JwtUtils;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

import static es.altia.domeadapter.backend.issuance.domain.Constants.*;
import static es.altia.domeadapter.backend.shared.domain.util.Constants.JWT_VC_JSON;
import static es.altia.domeadapter.backend.shared.domain.util.Constants.SYNC;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateLegacyIssuanceWorkflow {

    private final IssuerCoreClientPort issuerCoreClient;
    private final ProcedureRetryService procedureRetryService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    // todo remove
    private final M2MTokenService m2MTokenService;

    public Mono<Void> execute(PreSubmittedCredentialDataRequest request, String bearerToken, String idToken) {
        //todo remove
        return m2MTokenService.getM2MToken()
                .doOnNext(token -> log.info("[ISSUANCE] Temporary M2M token for testing: {}", token))
                .then(executeIssuance(request, bearerToken, idToken));

      
    }

    private Mono<Void> executeIssuance(PreSubmittedCredentialDataRequest request, String bearerToken, String idToken) {
        if (!JWT_VC_JSON.equals(request.format())) {
            return Mono.error(new FormatUnsupportedException("Format: " + request.format() + " is not supported"));
        }

        if (!request.operationMode().equals(SYNC)) {
            return Mono.error(new OperationNotSupportedException("operation_mode: " + request.operationMode() + " with schema: " + request.schema()));
        }

        if (!isSupportedSchema(request.schema())) {
            return Mono.error(new UnsupportedCredentialSchemaException(
                    "Unsupported credential schema: '" + request.schema() + "'. Supported schemas are: "
                            + LABEL_CREDENTIAL_SCHEMA + ", " + LEAR_CREDENTIAL_EMPLOYEE_SCHEMA + ", " + LEAR_CREDENTIAL_MACHINE_SCHEMA));
        }

        if (isLabelCredentialSchema(request.schema()) && (idToken == null || idToken.isBlank())) {
            return Mono.error(new MissingIdTokenHeaderException("Missing required ID Token header for Label credential issuance."));
        }

        try {
            validatePayload(request);
        } catch (InvalidCredentialFormatException e) {
            return Mono.error(e);
        }

        String delivery = resolveDelivery(request);
        String email;
        try {
            email = resolveEmail(request);
        } catch (MissingEmailOwnerException e) {
            return Mono.error(e);
        }

        ExternalPreSubmittedCredentialDataRequest externalRequest =
                ExternalPreSubmittedCredentialDataRequest.builder()
                        .schema(resolveExternalSchema(request.schema()))
                        .payload(request.payload())
                        .operationMode(request.operationMode())
                        .email(email)
                        .delivery(delivery)
                        .build();

        return issuerCoreClient.forward(externalRequest, bearerToken, idToken)
                .flatMap(response -> {
                    if (!isLabelCredentialSchema(request.schema())) {
                        return Mono.empty();
                    }

                    String signedCredential = response.signedCredential();
                    if (signedCredential == null || signedCredential.isBlank()) {
                        log.error("[ISSUANCE] External issuer returned empty credential");
                        return Mono.empty();
                    }

                    UUID credentialId = jwtUtils.extractCredentialId(signedCredential);
                    String productSpecificationId = jwtUtils.extractCredentialSubjectId(signedCredential);
                    log.info("[ISSUANCE] Label credential issued with id={} productSpecId={}", credentialId, productSpecificationId);

                    LabelCredentialDeliveryPayload payload = LabelCredentialDeliveryPayload.builder()
                            .responseUri(request.responseUri())
                            .credentialId(credentialId.toString())
                            .productSpecificationId(productSpecificationId)
                            .email(email)
                            .signedCredential(signedCredential)
                            .build();

                    log.debug("Label delivery payload: {}", payload);

                    log.info("[ISSUANCE] Firing delivery pipeline for label credential with credentialId={} productSpecId={}",
                            credentialId, productSpecificationId);

                    procedureRetryService
                            .handleInitialAction(credentialId, ActionType.UPLOAD_LABEL_TO_RESPONSE_URI, payload)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    e -> log.error("[ISSUANCE] Error during delivery pipeline for credentialId={}: {}", credentialId, e.getMessage())
                            );

                    return Mono.empty();
                });
    }

    private void validatePayload(PreSubmittedCredentialDataRequest request) {
        if (LABEL_CREDENTIAL_SCHEMA.equals(request.schema())) {
            validateLabelCredentialPayload(request.payload());
        } else if (LEAR_CREDENTIAL_EMPLOYEE_SCHEMA.equals(request.schema())) {
            validateLearCredentialEmployeePayload(request.payload());
        } else if (LEAR_CREDENTIAL_MACHINE_SCHEMA.equals(request.schema())) {
            validateLearCredentialMachinePayload(request.payload());
        }
    }

    private void validateLabelCredentialPayload(JsonNode payload) {
        log.debug("Validating LabelCredential payload: {}", payload);
        try {
            LabelCredential credential = objectMapper.convertValue(payload, LabelCredential.class);
            var violations = validator.validate(credential);
            if (!violations.isEmpty()) {
                throw new InvalidCredentialFormatException("Invalid LabelCredential payload");
            }
        } catch (IllegalArgumentException e) {
            log.error("Error mapping LabelCredential payload", e);
            throw new InvalidCredentialFormatException("Invalid LabelCredential payload");
        }
    }

    private void validateLearCredentialEmployeePayload(JsonNode payload) {
        try {
            objectMapper.convertValue(payload, LEARCredentialEmployee.CredentialSubject.Mandate.class);
        } catch (IllegalArgumentException e) {
            log.error("Error mapping LEARCredentialEmployee payload", e);
            throw new InvalidCredentialFormatException("Invalid LEARCredentialEmployee payload");
        }
    }

    private void validateLearCredentialMachinePayload(JsonNode payload) {
        try {
            objectMapper.convertValue(payload, LEARCredentialMachine.CredentialSubject.Mandate.class);
        } catch (IllegalArgumentException e) {
            log.error("Error mapping LEARCredentialMachine payload", e);
            throw new InvalidCredentialFormatException("Invalid LEARCredentialMachine payload");
        }
    }

    private boolean isSupportedSchema(String schema) {
        return LABEL_CREDENTIAL_SCHEMA.equals(schema)
                || LEAR_CREDENTIAL_EMPLOYEE_SCHEMA.equals(schema)
                || LEAR_CREDENTIAL_MACHINE_SCHEMA.equals(schema);
    }

    private String resolveExternalSchema(String schema) {
        return switch (schema) {
            case LABEL_CREDENTIAL_SCHEMA -> EXTERNAL_LABEL_CREDENTIAL_SCHEMA;
            case LEAR_CREDENTIAL_EMPLOYEE_SCHEMA -> EXTERNAL_LEAR_CREDENTIAL_EMPLOYEE_SCHEMA;
            case LEAR_CREDENTIAL_MACHINE_SCHEMA -> EXTERNAL_LEAR_CREDENTIAL_MACHINE_SCHEMA;
            default -> throw new UnsupportedCredentialSchemaException("Unsupported credential schema: " + schema);
        };
    }

    private String resolveDelivery(PreSubmittedCredentialDataRequest request) {
        if (request.delivery() != null && !request.delivery().isBlank()) {
            return request.delivery();
        }

        if (isLabelCredentialSchema(request.schema())) {
            return DEFAULT_LABEL_DELIVERY;
        }

        return DEFAULT_DELIVERY;
    }

    private boolean isLabelCredentialSchema(String schema) {
        return LABEL_CREDENTIAL_SCHEMA.equals(schema) || EXTERNAL_LABEL_CREDENTIAL_SCHEMA.equals(schema);
    }

    private String resolveEmail(PreSubmittedCredentialDataRequest request) {
        if (LABEL_CREDENTIAL_SCHEMA.equals(request.schema())) {
            if (request.email() == null || request.email().isBlank()) {
                throw new MissingEmailOwnerException("Email is required for Label credential issuance.");
            }
            return request.email();
        }

        if (LEAR_CREDENTIAL_MACHINE_SCHEMA.equals(request.schema())) {
            if (request.email() != null && !request.email().isBlank()) {
                return request.email();
            }
            JsonNode emailNode = request.payload().path("mandator").path("email");
            return emailNode.isMissingNode() || emailNode.isNull() ? null : emailNode.asText();
        }

        if (LEAR_CREDENTIAL_EMPLOYEE_SCHEMA.equals(request.schema())) {
            JsonNode emailNode = request.payload().path("mandatee").path("email");
            return emailNode.isMissingNode() || emailNode.isNull() ? null : emailNode.asText();
        }

        return request.email();
    }
}