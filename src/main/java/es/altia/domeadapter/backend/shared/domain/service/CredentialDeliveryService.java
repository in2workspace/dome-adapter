package es.altia.domeadapter.shared.domain.service;

import es.altia.domeadapter.shared.domain.model.dto.ResponseUriDeliveryResult;
import reactor.core.publisher.Mono;

public interface CredentialDeliveryService {
    Mono<ResponseUriDeliveryResult> deliverLabelToResponseUri(String responseUri, String encodedVc, String credId, String bearerToken);
}
