package es.altia.domeadapter.backend.issuance.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import es.altia.domeadapter.backend.issuance.domain.service.ExternalIssuanceService;
import es.altia.domeadapter.backend.shared.domain.model.dto.ExternalPreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.IssuanceResponse;
import es.altia.domeadapter.backend.shared.domain.model.dto.PreSubmittedCredentialDataRequest;
import es.altia.domeadapter.backend.shared.domain.model.dto.retry.LabelCredentialDeliveryPayload;
import es.altia.domeadapter.backend.shared.domain.model.enums.ActionType;
import es.altia.domeadapter.backend.shared.domain.service.ProcedureRetryService;
import es.altia.domeadapter.backend.shared.domain.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuanceWorkflowTest {

    @Mock
    private ExternalIssuanceService externalIssuanceService;
    @Mock
    private ProcedureRetryService procedureRetryService;
    @Mock
    private JwtUtils jwtUtils;

    private IssuanceWorkflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new IssuanceWorkflow(externalIssuanceService, procedureRetryService, jwtUtils);
    }

    @Test
    void execute_nonLabelCredential_forwardsAndReturnsEmpty() {
        when(externalIssuanceService.forward(any(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().credentialOfferUri("https://issuer/offer").build()));

        StepVerifier.create(workflow.execute(buildRequest("LEARCredentialEmployee", null), "token", "idToken"))
                .verifyComplete();

        verifyNoInteractions(procedureRetryService, jwtUtils);
    }

    @Test
    void execute_labelCredential_withSignedCredential_triggersDeliveryPipeline() {
        UUID credentialId = UUID.randomUUID();
        String productSpecId = "https://example.com/product/123";
        String signedCredential = "header.payload.sig";

        when(externalIssuanceService.forward(any(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().signedCredential(signedCredential).build()));
        when(jwtUtils.extractCredentialId(signedCredential)).thenReturn(credentialId);
        when(jwtUtils.extractCredentialSubjectId(signedCredential)).thenReturn(productSpecId);
        when(procedureRetryService.handleInitialAction(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(workflow.execute(buildRequest("gx:LabelCredential", "https://response.uri"), "token", "idToken"))
                .verifyComplete();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(procedureRetryService).handleInitialAction(
                eq(credentialId),
                eq(ActionType.UPLOAD_LABEL_TO_RESPONSE_URI),
                payloadCaptor.capture()
        );

        LabelCredentialDeliveryPayload payload = (LabelCredentialDeliveryPayload) payloadCaptor.getValue();
        assertThat(payload.credentialId()).isEqualTo(credentialId.toString());
        assertThat(payload.productSpecificationId()).isEqualTo(productSpecId);
        assertThat(payload.signedCredential()).isEqualTo(signedCredential);
        assertThat(payload.responseUri()).isEqualTo("https://response.uri");
        assertThat(payload.email()).isEqualTo("test@example.com");
    }

    @Test
    void execute_labelCredential_nullSignedCredential_skipsDeliveryPipeline() {
        when(externalIssuanceService.forward(any(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().signedCredential(null).build()));

        StepVerifier.create(workflow.execute(buildRequest("gx:LabelCredential", null), "token", "idToken"))
                .verifyComplete();

        verifyNoInteractions(procedureRetryService, jwtUtils);
    }

    @Test
    void execute_labelCredential_blankSignedCredential_skipsDeliveryPipeline() {
        when(externalIssuanceService.forward(any(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().signedCredential("  ").build()));

        StepVerifier.create(workflow.execute(buildRequest("gx:LabelCredential", null), "token", "idToken"))
                .verifyComplete();

        verifyNoInteractions(procedureRetryService, jwtUtils);
    }

    @Test
    void execute_schemaMapping_labelCredential_mapsToExternal() {
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(buildRequest("gx:LabelCredential", null), "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().schema()).isEqualTo("gx.labelcredential.w3c.1");
    }

    @Test
    void execute_schemaMapping_learCredentialEmployee_mapsToExternal() {
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(buildRequest("LEARCredentialEmployee", null), "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().schema()).isEqualTo("learcredential.employee.w3c.3.json");
    }

    @Test
    void execute_schemaMapping_unknownSchema_passedThrough() {
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(buildRequest("customSchema", null), "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().schema()).isEqualTo("customSchema");
    }

    @Test
    void execute_delivery_labelCredential_noExplicit_usesDefaultLabelDelivery() {
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(buildRequest("gx:LabelCredential", null), "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().delivery()).isEqualTo("email,direct");
    }

    @Test
    void execute_delivery_nonLabelCredential_noExplicit_usesDefaultDelivery() {
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(buildRequest("LEARCredentialEmployee", null), "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().delivery()).isEqualTo("email");
    }

    @Test
    void execute_delivery_explicit_usesProvidedDelivery() {
        PreSubmittedCredentialDataRequest request = PreSubmittedCredentialDataRequest.builder()
                .schema("LEARCredentialEmployee")
                .format("jwt_vc")
                .payload(JsonNodeFactory.instance.objectNode())
                .email("test@example.com")
                .delivery("sms")
                .build();
        ArgumentCaptor<ExternalPreSubmittedCredentialDataRequest> captor =
                ArgumentCaptor.forClass(ExternalPreSubmittedCredentialDataRequest.class);
        when(externalIssuanceService.forward(captor.capture(), anyString(), anyString()))
                .thenReturn(Mono.just(IssuanceResponse.builder().build()));

        StepVerifier.create(workflow.execute(request, "token", "idToken"))
                .verifyComplete();

        assertThat(captor.getValue().delivery()).isEqualTo("sms");
    }

    private PreSubmittedCredentialDataRequest buildRequest(String schema, String responseUri) {
        return PreSubmittedCredentialDataRequest.builder()
                .schema(schema)
                .format("jwt_vc")
                .payload(JsonNodeFactory.instance.objectNode())
                .email("test@example.com")
                .responseUri(responseUri)
                .build();
    }
}
