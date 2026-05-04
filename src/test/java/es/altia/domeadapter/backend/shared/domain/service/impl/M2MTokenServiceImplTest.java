package es.altia.domeadapter.backend.shared.domain.service.impl;

import es.altia.domeadapter.backend.shared.domain.model.dto.VerifierOauth2AccessToken;
import es.altia.domeadapter.backend.shared.domain.service.JWTService;
import es.altia.domeadapter.backend.shared.domain.service.VerifierService;
import es.altia.domeadapter.backend.shared.infrastructure.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class M2MTokenServiceImplTest {

    @Mock private JWTService jwtService;
    @Mock private AppConfig appConfig;
    @Mock private VerifierService verifierService;

    @InjectMocks
    private M2MTokenServiceImpl m2mTokenService;

    private static final String CLIENT_ID = "did:key:zABC123";
    private static final String VERIFIER_URL = "https://verifier.example.com";
    private static final String JWT_CREDENTIAL_BASE64 =
            Base64.getEncoder().encodeToString("fake-vc-jwt-string".getBytes());

    @BeforeEach
    void setUp() {
        when(appConfig.getCredentialSubjectDidKey()).thenReturn(CLIENT_ID);
        when(appConfig.getVerifierUrl()).thenReturn(VERIFIER_URL);
        when(appConfig.getJwtCredential()).thenReturn(JWT_CREDENTIAL_BASE64);
        when(jwtService.generateJWT(anyString())).thenReturn("signed-jwt-token");
    }

    @Test
    void getM2MToken_happyPath_returnsAccessToken() {
        VerifierOauth2AccessToken expectedToken = VerifierOauth2AccessToken.builder()
                .accessToken("m2m-access-token")
                .tokenType("Bearer")
                .expiresIn("3600")
                .build();
        when(verifierService.performTokenRequest(anyString())).thenReturn(Mono.just(expectedToken));

        StepVerifier.create(m2mTokenService.getM2MToken())
                .expectNext(expectedToken)
                .verifyComplete();
    }

    @Test
    void getM2MToken_buildsTwoJwts_vpTokenAndClientAssertion() {
        when(verifierService.performTokenRequest(anyString())).thenReturn(Mono.just(
                VerifierOauth2AccessToken.builder().accessToken("token").build()
        ));

        m2mTokenService.getM2MToken().block();

        verify(jwtService, times(2)).generateJWT(anyString());
    }

    @Test
    void getM2MToken_formBodyContainsRequiredParams() {
        when(verifierService.performTokenRequest(anyString())).thenReturn(Mono.just(
                VerifierOauth2AccessToken.builder().accessToken("token").build()
        ));

        m2mTokenService.getM2MToken().block();

        verify(verifierService).performTokenRequest(argThat(body ->
                body.contains("grant_type=client_credentials") &&
                body.contains("client_id=" + CLIENT_ID) &&
                body.contains("client_assertion_type=") &&
                body.contains("client_assertion=signed-jwt-token")
        ));
    }

    @Test
    void getM2MToken_whenVerifierFails_propagatesError() {
        when(verifierService.performTokenRequest(anyString()))
                .thenReturn(Mono.error(new RuntimeException("verifier unavailable")));

        StepVerifier.create(m2mTokenService.getM2MToken())
                .expectError(RuntimeException.class)
                .verify();
    }
}
