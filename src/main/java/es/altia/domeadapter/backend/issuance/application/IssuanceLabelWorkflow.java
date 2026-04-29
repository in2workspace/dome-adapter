package es.altia.domeadapter.issuance.application;

import es.altia.domeadapter.issuance.infrastructure.service.ExternalIssuanceService;
import es.altia.domeadapter.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import es.altia.domeadapter.shared.domain.model.dto.retry.LabelCredentialDeliveryPayload;
import es.altia.domeadapter.shared.domain.model.enums.ActionType;
import es.altia.domeadapter.shared.domain.service.ProcedureRetryService;
import es.altia.domeadapter.shared.domain.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuanceLabelWorkflow {

    private final ExternalIssuanceService externalIssuanceService;
    private final ProcedureRetryService procedureRetryService;
    private final JwtUtils jwtUtils;

    public Mono<Void> execute(PreSubmittedCredentialDataRequest request, String bearerToken) {
        return externalIssuanceService.forward(request, bearerToken)
                .flatMap(response -> {
                    String signedCredential = response.credential();
                    if (signedCredential == null || signedCredential.isBlank()) {
                        log.error("[ISSUANCE] External issuer returned empty credential");
                        return Mono.empty();
                    }

                    UUID credentialId = jwtUtils.extractCredentialId(signedCredential);
                    String productSpecificationId = jwtUtils.extractCredentialSubjectId(signedCredential);

                    LabelCredentialDeliveryPayload payload = LabelCredentialDeliveryPayload.builder()
                            .responseUri(request.responseUri())
                            .credentialId(credentialId.toString())
                            .productSpecificationId(productSpecificationId)
                            .email(request.email())
                            .signedCredential(signedCredential)
                            .build();

                    log.info("[ISSUANCE] Firing delivery pipeline for credentialId={} productSpecId={}",
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
}
