package es.altia.domeadapter.backend.shared.infrastructure.security;

import es.altia.domeadapter.backend.shared.infrastructure.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static es.altia.domeadapter.backend.shared.domain.util.EndpointsConstants.ISSUANCES_PATH;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppConfig appConfig;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration issuancesConfig = new CorsConfiguration();
        issuancesConfig.setAllowedOrigins(appConfig.getExternalCorsAllowedOrigins());
        issuancesConfig.setAllowedMethods(List.of("POST", "OPTIONS"));
        issuancesConfig.setAllowedHeaders(List.of("*"));
        issuancesConfig.setAllowCredentials(false);
        issuancesConfig.setMaxAge(1800L);
        source.registerCorsConfiguration(ISSUANCES_PATH, issuancesConfig);

        return source;
    }
}
