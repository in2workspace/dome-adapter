package es.altia.domeadapter.backend.issuance.domain.service;

import es.altia.domeadapter.backend.shared.domain.model.dto.ExternalPreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.IssuanceResponse;
import reactor.core.publisher.Mono;

public interface IssuerCoreClientPort {

    Mono<IssuanceResponse> forward(ExternalPreSubmittedCredentialDataRequest request, String bearerToken, String idToken);
}