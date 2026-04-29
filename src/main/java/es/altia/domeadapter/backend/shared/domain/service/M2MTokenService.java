package es.altia.domeadapter.shared.domain.service;

import es.altia.domeadapter.shared.domain.model.dto.VerifierOauth2AccessToken;
import reactor.core.publisher.Mono;

public interface M2MTokenService {
    Mono<VerifierOauth2AccessToken> getM2MToken();
}
