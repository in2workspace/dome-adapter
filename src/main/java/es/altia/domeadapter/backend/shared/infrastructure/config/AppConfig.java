package es.altia.domeadapter.backend.shared.infrastructure.config;

import es.altia.domeadapter.backend.shared.infrastructure.config.properties.IssuerIdentityProperties;
import es.altia.domeadapter.backend.shared.infrastructure.config.adapter.ConfigAdapter;
import es.altia.domeadapter.backend.shared.infrastructure.config.adapter.factory.ConfigAdapterFactory;
import es.altia.domeadapter.backend.shared.infrastructure.config.properties.AppProperties;
import es.altia.domeadapter.backend.shared.infrastructure.config.properties.CorsProperties;
import es.altia.domeadapter.backend.shared.infrastructure.config.properties.RetryProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    private final ConfigAdapter configAdapter;
    private final AppProperties appProperties;
    private final IssuerIdentityProperties issuerIdentityProperties;
    private final RetryProperties retryProperties;
    private final CorsProperties corsProperties;

    public AppConfig(
            ConfigAdapterFactory configAdapterFactory,
            AppProperties appProperties,
            IssuerIdentityProperties issuerIdentityProperties,
            RetryProperties retryProperties,
            CorsProperties corsProperties
    ) {
        this.configAdapter              = configAdapterFactory.getAdapter();
        this.appProperties              = appProperties;
        this.issuerIdentityProperties   = issuerIdentityProperties;
        this.retryProperties            = retryProperties;
        this.corsProperties             = corsProperties;
    }

    public String getVerifierUrl() {
        return configAdapter.getConfiguration(appProperties.verifierUrl());
    }

    /** URL of the external issuer — used to forward requests and to validate issuer-signed tokens. */
    public String getExternalIssuerUrl() {
        return configAdapter.getConfiguration(appProperties.issuerUrl());
    }

    public String getDefaultLang() {
        return configAdapter.getConfiguration(appProperties.defaultLang());
    }

    public String getConfigSource() {
        return configAdapter.getConfiguration(appProperties.configSource());
    }

    public String getCredentialSubjectDidKey() {
        return issuerIdentityProperties.credentialSubjectDidKey();
    }

    public String getJwtCredential() {
        return issuerIdentityProperties.jwtCredential();
    }

    public String getCryptoPrivateKey() {
        return issuerIdentityProperties.crypto().privateKey();
    }

    public String getKnowledgeBaseUploadCertificationGuideUrl() {
        return configAdapter.getConfiguration(retryProperties.knowledgeBase().uploadCertificationGuideUrl());
    }

    public String getLabelUploadCertifierEmail() {
        return retryProperties.labelUpload().certifierEmail();
    }

    public String getLabelUploadMarketplaceEmail() {
        return retryProperties.labelUpload().marketplaceEmail();
    }

    public List<String> getExternalCorsAllowedOrigins() {
        return corsProperties.externalAllowedOrigins();
    }
}
