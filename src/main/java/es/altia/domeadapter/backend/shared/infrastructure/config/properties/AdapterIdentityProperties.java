package es.altia.domeadapter.backend.shared.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "adapter-identity")
@Validated
public record AdapterIdentityProperties(
        @NotBlank String credentialSubjectDidKey,
        @NotBlank String jwtCredential,
        @NotNull Crypto crypto
) {
    @ConstructorBinding
    public AdapterIdentityProperties(String credentialSubjectDidKey, String jwtCredential, Crypto crypto) {
        this.credentialSubjectDidKey = credentialSubjectDidKey;
        this.jwtCredential           = jwtCredential;
        this.crypto                  = crypto;
    }

    @Validated
    public record Crypto(@NotBlank String privateKey) {
    }
}
