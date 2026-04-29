package es.altia.domeadapter.backend.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public final class DualTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String ID_TOKEN_HEADER = "X-ID-Token";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        log.debug("DualTokenServerAuthenticationConverter - convert -> [{} {}]",
                request.getMethod(), request.getPath());

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Mono.empty();
        }
        String accessToken = auth.substring(7).trim();
        String idToken = request.getHeaders().getFirst(ID_TOKEN_HEADER);
        return Mono.just(new DualTokenAuthentication(
                accessToken,
                (idToken == null || idToken.isBlank()) ? null : idToken
        ));
    }
}
