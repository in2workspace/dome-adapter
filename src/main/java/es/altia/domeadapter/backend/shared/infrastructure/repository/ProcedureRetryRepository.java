package es.altia.domeadapter.shared.infrastructure.repository;

import es.altia.domeadapter.shared.domain.model.entities.ProcedureRetry;
import es.altia.domeadapter.shared.domain.model.enums.ActionType;
import es.altia.domeadapter.shared.domain.model.enums.RetryStatus;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ProcedureRetryRepository extends ReactiveCrudRepository<ProcedureRetry, UUID> {

    Flux<ProcedureRetry> findByStatus(RetryStatus status);

    Mono<ProcedureRetry> findByCredentialIdAndActionType(UUID credentialId, ActionType actionType);

    @Query("SELECT * FROM dome_adapter.procedure_retry WHERE status = 'PENDING' AND first_failure_at < :exhaustionThreshold")
    Flux<ProcedureRetry> findPendingRecordsOlderThan(Instant exhaustionThreshold);

    @Modifying
    @Query("""
            INSERT INTO dome_adapter.procedure_retry
                (id, credential_id, action_type, status, attempt_count, first_failure_at, payload)
            VALUES
                (:#{#retry.id}, :#{#retry.credentialId}, :#{#retry.actionType}, :#{#retry.status},
                 :#{#retry.attemptCount}, :#{#retry.firstFailureAt}, :#{#retry.payload})
            ON CONFLICT (credential_id, action_type)
            DO UPDATE SET
                status = EXCLUDED.status,
                payload = EXCLUDED.payload
            """)
    Mono<Integer> upsert(ProcedureRetry retry);

    @Modifying
    @Query("""
            UPDATE dome_adapter.procedure_retry
            SET attempt_count = attempt_count + 1,
                last_attempt_at = :lastAttemptAt
            WHERE credential_id = :credentialId AND action_type = :actionType
            """)
    Mono<Integer> incrementAttemptCount(UUID credentialId, ActionType actionType, Instant lastAttemptAt);

    @Modifying
    @Query("""
            UPDATE dome_adapter.procedure_retry
            SET status = 'COMPLETED'
            WHERE credential_id = :credentialId AND action_type = :actionType
            """)
    Mono<Integer> markAsCompleted(UUID credentialId, ActionType actionType);

    @Modifying
    @Query("""
            UPDATE dome_adapter.procedure_retry
            SET status = 'RETRY_EXHAUSTED'
            WHERE credential_id = :credentialId AND action_type = :actionType
            """)
    Mono<Integer> markAsExhausted(UUID credentialId, ActionType actionType);
}
