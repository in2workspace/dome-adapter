package es.altia.domeadapter.backend.issuance.domain.service;

import es.altia.domeadapter.backend.shared.domain.model.dto.ExternalPreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.IssuanceResponse;
import es.altia.domeadapter.backend.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import reactor.core.publisher.Mono;

public interface ExternalIssuanceService {

    Mono<IssuanceResponse> forward(ExternalPreSubmittedCredentialDataRequest request, String bearerToken, String idToken);
}