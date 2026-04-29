package es.altia.domeadapter.backend.shared.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> externalAllowedOrigins) {

    @ConstructorBinding
    public CorsProperties(List<String> externalAllowedOrigins) {
        this.externalAllowedOrigins = Optional.ofNullable(externalAllowedOrigins).orElse(List.of());
    }
}
