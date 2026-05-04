package es.altia.domeadapter.backend.shared.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils(new ObjectMapper());

    @Test
    void decodePayloadShouldReturnDecodedPayload() {
        String payload = """
                {
                  "sub": "user-1",
                  "name": "Test User"
                }
                """;

        String jwt = buildJwt(payload);

        String decodedPayload = jwtUtils.decodePayload(jwt);

        assertThat(decodedPayload).isEqualTo(payload);
    }

    @Test
    void decodePayloadShouldThrowIllegalArgumentExceptionWhenJwtHasInvalidFormat() {
        assertThatThrownBy(() -> jwtUtils.decodePayload("invalid-jwt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT format");
    }

    @Test
    void extractCredentialIdShouldReturnJtiWhenPresent() {
        UUID credentialId = UUID.randomUUID();

        String jwt = buildJwt("""
                {
                  "jti": "%s",
                  "id": "%s"
                }
                """.formatted(credentialId, UUID.randomUUID()));

        UUID result = jwtUtils.extractCredentialId(jwt);

        assertThat(result).isEqualTo(credentialId);
    }

    @Test
    void extractCredentialIdShouldReturnIdWhenJtiIsMissing() {
        UUID credentialId = UUID.randomUUID();

        String jwt = buildJwt("""
                {
                  "id": "%s"
                }
                """.formatted(credentialId));

        UUID result = jwtUtils.extractCredentialId(jwt);

        assertThat(result).isEqualTo(credentialId);
    }

    @Test
    void extractCredentialIdShouldReturnVcIdWhenRootIdAndJtiAreMissing() {
        UUID credentialId = UUID.randomUUID();

        String jwt = buildJwt("""
                {
                  "vc": {
                    "id": "%s"
                  }
                }
                """.formatted(credentialId));

        UUID result = jwtUtils.extractCredentialId(jwt);

        assertThat(result).isEqualTo(credentialId);
    }

    @Test
    void extractCredentialIdShouldReturnUuidFromUrnUuidValue() {
        UUID credentialId = UUID.randomUUID();

        String jwt = buildJwt("""
                {
                  "jti": "urn:uuid:%s"
                }
                """.formatted(credentialId));

        UUID result = jwtUtils.extractCredentialId(jwt);

        assertThat(result).isEqualTo(credentialId);
    }

    @Test
    void extractCredentialIdShouldIgnoreInvalidJtiAndReturnValidRootId() {
        UUID credentialId = UUID.randomUUID();

        String jwt = buildJwt("""
                {
                  "jti": "invalid-uuid",
                  "id": "%s"
                }
                """.formatted(credentialId));

        UUID result = jwtUtils.extractCredentialId(jwt);

        assertThat(result).isEqualTo(credentialId);
    }

    @Test
    void extractCredentialIdShouldThrowIllegalArgumentExceptionWhenNoCredentialIdCanBeExtracted() {
        String jwt = buildJwt("""
                {
                  "jti": "invalid-uuid",
                  "id": "",
                  "vc": {
                    "id": "also-invalid"
                  }
                }
                """);

        assertThatThrownBy(() -> jwtUtils.extractCredentialId(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Could not extract credential ID from JWT claims");
    }

    @Test
    void extractCredentialIdShouldThrowIllegalArgumentExceptionWhenPayloadIsNotValidJson() {
        String jwt = buildJwt("not-json");

        assertThatThrownBy(() -> jwtUtils.extractCredentialId(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Error extracting credential ID from JWT");
    }

    @Test
    void extractCredentialSubjectIdShouldReturnRootCredentialSubjectIdWhenPresent() {
        String subjectId = "did:example:subject-1";

        String jwt = buildJwt("""
                {
                  "credentialSubject": {
                    "id": "%s"
                  },
                  "vc": {
                    "credentialSubject": {
                      "id": "did:example:subject-2"
                    }
                  }
                }
                """.formatted(subjectId));

        String result = jwtUtils.extractCredentialSubjectId(jwt);

        assertThat(result).isEqualTo(subjectId);
    }

    @Test
    void extractCredentialSubjectIdShouldReturnVcCredentialSubjectIdWhenRootCredentialSubjectIdIsMissing() {
        String subjectId = "did:example:subject-2";

        String jwt = buildJwt("""
                {
                  "vc": {
                    "credentialSubject": {
                      "id": "%s"
                    }
                  }
                }
                """.formatted(subjectId));

        String result = jwtUtils.extractCredentialSubjectId(jwt);

        assertThat(result).isEqualTo(subjectId);
    }

    @Test
    void extractCredentialSubjectIdShouldReturnBlankWhenCredentialSubjectIdIsMissing() {
        String jwt = buildJwt("""
                {
                  "vc": {
                    "credentialSubject": {}
                  }
                }
                """);

        String result = jwtUtils.extractCredentialSubjectId(jwt);

        assertThat(result).isBlank();
    }

    @Test
    void extractCredentialSubjectIdShouldReturnBlankWhenJwtIsInvalid() {
        String result = jwtUtils.extractCredentialSubjectId("invalid-jwt");

        assertThat(result).isBlank();
    }

    private static String buildJwt(String payload) {
        String header = """
                {
                  "alg": "none",
                  "typ": "JWT"
                }
                """;

        return base64UrlEncode(header) + "." + base64UrlEncode(payload) + ".signature";
    }

    private static String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
