package es.altia.domeadapter.shared.domain.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final ObjectMapper objectMapper;

    public String decodePayload(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public UUID extractCredentialId(String jwt) {
        try {
            JsonNode claims = objectMapper.readTree(decodePayload(jwt));

            JsonNode jtiNode = claims.get("jti");
            if (jtiNode != null && !jtiNode.isNull()) {
                String jti = jtiNode.asText();
                if (jti.startsWith("urn:uuid:")) {
                    return UUID.fromString(jti.substring("urn:uuid:".length()));
                }
                try { return UUID.fromString(jti); } catch (IllegalArgumentException ignored) { }
            }

            JsonNode vcNode = claims.get("vc");
            if (vcNode != null && !vcNode.isNull()) {
                JsonNode vcIdNode = vcNode.get("id");
                if (vcIdNode != null && !vcIdNode.isNull()) {
                    String vcId = vcIdNode.asText();
                    if (vcId.startsWith("urn:uuid:")) {
                        return UUID.fromString(vcId.substring("urn:uuid:".length()));
                    }
                    try { return UUID.fromString(vcId); } catch (IllegalArgumentException ignored) { }
                    return UUID.nameUUIDFromBytes(vcId.getBytes(StandardCharsets.UTF_8));
                }
            }

            log.warn("[JWT] Could not extract credential ID, generating synthetic UUID");
            return UUID.nameUUIDFromBytes(jwt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[JWT] Error extracting credential ID: {}", e.getMessage(), e);
            return UUID.nameUUIDFromBytes(jwt.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String extractCredentialSubjectId(String jwt) {
        try {
            JsonNode claims = objectMapper.readTree(decodePayload(jwt));
            JsonNode vcNode = claims.get("vc");
            if (vcNode != null && !vcNode.isNull()) {
                JsonNode csNode = vcNode.get("credentialSubject");
                if (csNode != null && !csNode.isNull()) {
                    JsonNode idNode = csNode.get("id");
                    if (idNode != null && !idNode.isNull()) {
                        return idNode.asText();
                    }
                }
            }
            log.warn("[JWT] Could not extract credentialSubject.id");
            return "";
        } catch (Exception e) {
            log.error("[JWT] Error extracting credentialSubject.id: {}", e.getMessage(), e);
            return "";
        }
    }
}
