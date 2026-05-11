package es.altia.domeadapter.backend.issuance.application;

import es.altia.domeadapter.backend.issuance.domain.service.IssuerCoreClientPort;
import es.altia.domeadapter.backend.shared.domain.exception.FormatUnsupportedException;
import es.altia.domeadapter.backend.shared.domain.model.dto.ExternalPreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.retry.LabelCredentialDeliveryPayload;
import es.altia.domeadapter.backend.shared.domain.model.enums.ActionType;
import es.altia.domeadapter.backend.shared.domain.service.ProcedureRetryService;
import es.altia.domeadapter.backend.shared.domain.util.JwtUtils;
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

    public Mono<Void> execute(PreSubmittedCredentialDataRequest request, String bearerToken, String idToken) {
        // Check if the format is not "json_vc_jwt"
        if (!JWT_VC_JSON.equals(request.format())) {
            return Mono.error(new FormatUnsupportedException("Format: " + request.format() + " is not supported"));
        }
        // Check if operation_mode is different to sync
        if (!request.operationMode().equals(SYNC)) {
            return Mono.error(new OperationNotSupportedException("operation_mode: " + request.operationMode() + " with schema: " + request.schema()));
        }
        ExternalPreSubmittedCredentialDataRequest externalRequest =
                ExternalPreSubmittedCredentialDataRequest.builder()
                        .schema(resolveExternalSchema(request.schema()))
                        .payload(request.payload()) //todo validate?
                        .operationMode(request.operationMode())
                        .email(request.email())
                        .delivery(resolveDelivery(request)) //todo
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
                            .email(request.email())
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

    private String resolveExternalSchema(String schema) {
        return switch (schema) {
            case LABEL_CREDENTIAL_SCHEMA -> EXTERNAL_LABEL_CREDENTIAL_SCHEMA;
            case LEAR_CREDENTIAL_EMPLOYEE_SCHEMA -> EXTERNAL_LEAR_CREDENTIAL_EMPLOYEE_SCHEMA;
            case LEAR_CREDENTIAL_MACHINE_SCHEMA -> EXTERNAL_LEAR_CREDENTIAL_MACHINE_SCHEMA;
            default -> schema; //todo error?
        };
    }

    private String resolveDelivery(PreSubmittedCredentialDataRequest request) {
        if (request.delivery() != null && !request.delivery().isBlank()) {
            return request.delivery();
        }

        if (isLabelCredentialSchema(request.schema())) {
            return DEFAULT_LABEL_DELIVERY;
        }

        return DEFAULT_DELIVERY; //todo comprovar si "ui" funciona amb l'adapter
    }

    private boolean isLabelCredentialSchema(String schema) {
        return LABEL_CREDENTIAL_SCHEMA.equals(schema) || EXTERNAL_LABEL_CREDENTIAL_SCHEMA.equals(schema);
    }
}