package es.altia.domeadapter.issuance.infrastructure.service.impl;

import es.altia.domeadapter.issuance.infrastructure.service.ExternalIssuanceService;
import es.altia.domeadapter.backend.shared.domain.model.dto.IssuanceResponse;
import es.altia.domeadapter.backend.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.ISSUANCES_PATH;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIssuanceServiceImpl implements ExternalIssuanceService {

    private final WebClient issuerWebClient;

    @Override
    public Mono<IssuanceResponse> forward(PreSubmittedCredentialDataRequest request, String bearerToken) {
        log.info("[ISSUANCE] Forwarding issuance request to external issuer");

        return issuerWebClient
                .post()
                .uri(ISSUANCES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(IssuanceResponse.class);
                    }

                    // Re-throw the issuer's error verbatim (status + body) so the caller of
                    // dome-adapter receives exactly what the issuer returned, instead of a
                    // generic 500 produced by Spring's default error handler.
                    return response.bodyToMono(byte[].class)
                            .defaultIfEmpty(new byte[0])
                            .flatMap(body -> Mono.error(
                                    WebClientResponseException.create(
                                            response.statusCode().value(),
                                            response.statusCode().toString(),
                                            response.headers().asHttpHeaders(),
                                            body,
                                            StandardCharsets.UTF_8
                                    )
                            ));
                });
    }
}