package es.altia.domeadapter.backend.shared.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.HEALTH_PATH;
import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.ISSUANCES_PATH;
import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.PROMETHEUS_PATH;
import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.SPRINGDOC_PATH;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationManager customAuthenticationManager;
    private final CorsConfig corsConfig;

    @Bean
    @Primary
    public ReactiveAuthenticationManager primaryAuthenticationManager() {
        return customAuthenticationManager;
    }

    @Bean
    public AuthenticationWebFilter customAuthenticationWebFilter(ProblemAuthenticationEntryPoint entryPoint) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(customAuthenticationManager);
        log.debug("customAuthenticationWebFilter - inside");

        authenticationWebFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(ISSUANCES_PATH)
        );

        authenticationWebFilter.setServerAuthenticationConverter(new DualTokenServerAuthenticationConverter());
        authenticationWebFilter.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(entryPoint));
        return authenticationWebFilter;
    }

    @Bean
    public SecurityWebFilterChain filterChain(
            ServerHttpSecurity http,
            ProblemAuthenticationEntryPoint entryPoint,
            ProblemAccessDeniedHandler deniedHandler
    ) {
        log.debug("filterChain - inside");

        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.GET, HEALTH_PATH, PROMETHEUS_PATH, SPRINGDOC_PATH).permitAll()
                        .pathMatchers(HttpMethod.POST, ISSUANCES_PATH).authenticated()
                        .anyExchange().denyAll()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAt(customAuthenticationWebFilter(entryPoint), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                );

        log.debug("filterChain - build");
        return http.build();
    }
}
