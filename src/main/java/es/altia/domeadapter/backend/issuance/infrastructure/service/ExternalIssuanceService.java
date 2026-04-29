package es.altia.domeadapter.issuance.infrastructure.service;

import es.altia.domeadapter.shared.domain.model.dto.IssuanceResponse;
import es.altia.domeadapter.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import reactor.core.publisher.Mono;

public interface ExternalIssuanceService {

    Mono<IssuanceResponse> forward(PreSubmittedCredentialDataRequest request, String bearerToken);
}