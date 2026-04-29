package es.altia.domeadapter.backend.shared.domain.service;

import es.altia.domeadapter.backend.shared.domain.model.dto.VerifierOauth2AccessToken;
import reactor.core.publisher.Mono;

public interface M2MTokenService {
    Mono<VerifierOauth2AccessToken> getM2MToken();
}
