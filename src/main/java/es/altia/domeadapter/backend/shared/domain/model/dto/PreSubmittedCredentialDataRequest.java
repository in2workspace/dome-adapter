package es.altia.domeadapter.backend.shared.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PreSubmittedCredentialDataRequest(
        @JsonProperty(value = "schema", required = true) String schema,
        @JsonProperty(value = "format", required = true) String format,
        @JsonProperty(value = "payload", required = true) JsonNode payload,
        @JsonProperty("operation_mode") String operationMode,
        @JsonProperty("response_uri") String responseUri,
        @JsonProperty("issuance_notification_uri") String issuanceNotificationUri,
        @NotBlank(message = "email is required")
        @JsonProperty("email") String email,
        @JsonProperty("delivery") String delivery
) {}
