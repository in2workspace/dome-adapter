package es.altia.domeadapter.backend.shared.domain.scheduler;

import es.altia.domeadapter.backend.shared.domain.service.ProcedureRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class RetryScheduler {

    private final ProcedureRetryService procedureRetryService;

    @Scheduled(initialDelay = 120000, fixedRate = 12 * 60 * 60 * 1000)
    public Mono<Void> processRetries() {
        log.info("[RETRY] [RetryScheduler] Scheduled Task - Executing retry processing at: {}", Instant.now());
        return procedureRetryService.processPendingRetries()
                .then(procedureRetryService.markRetryAsExhausted())
                .doOnSuccess(unused -> log.info("[RETRY] [RetryScheduler] Completed scheduled retry processing at {}", Instant.now()))
                .doOnError(e -> log.error("[RETRY] [RetryScheduler] ERROR during scheduled retry processing: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.empty());
    }
}
