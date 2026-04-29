package es.altia.domeadapter.backend.shared.domain.service;

import reactor.core.publisher.Mono;

public interface EmailService {
    Mono<Void> sendResponseUriFailed(String to, String productSpecificationId, String credentialId, String providerEmail, String guideUrl);
    Mono<Void> sendResponseUriExhausted(String to, String productSpecificationId, String credentialId, String providerEmail, String guideUrl);
    Mono<Void> sendCertificationUploaded(String to, String productSpecificationId, String credentialId);
    Mono<Void> sendResponseUriAcceptedWithHtml(String to, String productId, String htmlContent);
}
