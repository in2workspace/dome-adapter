package es.altia.domeadapter.shared.domain.service;

import es.altia.domeadapter.shared.domain.model.enums.ActionType;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

public interface ProcedureRetryService {

    Mono<Void> handleInitialAction(UUID credentialId, ActionType actionType, Object payload);

    Mono<Void> createRetryRecord(UUID credentialId, ActionType actionType, Object payload);

    Mono<Void> retryAction(UUID credentialId, ActionType actionType);

    Mono<Void> markRetryAsCompleted(UUID credentialId, ActionType actionType);

    Mono<Void> markRetryAsExhausted();

    Mono<Void> markRetryAsExhausted(Duration customExhaustionThreshold);

    Mono<Void> processPendingRetries();
}
