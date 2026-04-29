package es.altia.domeadapter.backend.shared.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @NotBlank @URL String verifierUrl,
        @NotBlank @URL String issuerUrl,
        @NotBlank String defaultLang,
        @NotBlank String configSource
) {
    @ConstructorBinding
    public AppProperties(String verifierUrl, String issuerUrl, String defaultLang, String configSource) {
        this.verifierUrl  = verifierUrl;
        this.issuerUrl    = issuerUrl;
        this.defaultLang  = defaultLang;
        this.configSource = configSource;
    }
}
