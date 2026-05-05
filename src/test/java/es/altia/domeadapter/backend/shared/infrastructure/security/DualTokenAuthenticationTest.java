package es.altia.domeadapter.backend.shared.infrastructure.security;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DualTokenAuthenticationTest {

    @Test
    void constructor_setsFieldsAndDefaultAuthenticationState() {
        String accessToken = "access-123";
        String idToken = "id-456";
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication auth = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication(accessToken, idToken);
        // Access token is exposed via getCredentials()
        assertEquals(accessToken, auth.getCredentials());
        assertEquals(idToken, auth.getIdToken());
        assertEquals("N/A", auth.getPrincipal());
        assertFalse(auth.isAuthenticated(), "New DualTokenAuthentication should be unauthenticated by default");

        assertNotNull(auth.getAuthorities());
        assertTrue(auth.getAuthorities().isEmpty(), "Authorities should be empty when constructed");
    }

    @Test
    void constructor_withNullIdToken_allowsNullValue() {
        String accessToken = "access-only";

        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication auth = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication(accessToken, null);

        assertEquals(accessToken, auth.getCredentials());
        assertNull(auth.getIdToken());
        assertFalse(auth.isAuthenticated());
    }

    @Test
    void canChangeAuthenticatedFlag() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication auth = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("a", null);
        assertFalse(auth.isAuthenticated());

        auth.setAuthenticated(true);
        assertTrue(auth.isAuthenticated());

        auth.setAuthenticated(false);
        assertFalse(auth.isAuthenticated());
    }

    // ---------- Overrides coverage below ----------

    @Test
    void getCredentials_and_getPrincipal_areOverridden() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication auth = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("access-xyz", "id-xyz");
        assertEquals("access-xyz", auth.getCredentials());
        assertEquals("N/A", auth.getPrincipal());
    }

    @Test
    void getIdToken_and_getCredentials_returnValues() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication withBoth = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("A", "I");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication withOnlyAccess = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("A", null);

        assertEquals("I", withBoth.getIdToken());
        assertEquals("A", withBoth.getCredentials());

        assertNull(withOnlyAccess.getIdToken());
        assertEquals("A", withOnlyAccess.getCredentials());
    }

    @Test
    void equals_reflexive_symmetric_transitive_and_hashCode_consistent_forSameTokens() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a1 = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a2 = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a3 = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");

        // Reflexive
        assertEquals(a1, a1);
        // Symmetric
        assertEquals(a1, a2);
        assertEquals(a2, a1);
        // Transitive
        assertEquals(a2, a3);
        assertEquals(a1, a3);

        // hashCode consistency
        assertEquals(a1.hashCode(), a2.hashCode());
        assertEquals(a2.hashCode(), a3.hashCode());
    }

    @Test
    void equals_returnsFalse_whenComparedWithNullOrDifferentType() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        assertNotEquals(null, a);
        assertNotEquals("not-an-auth-object", a);
    }

    @Test
    void equals_returnsFalse_whenAccessTokenDiffers() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc1", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication b = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc2", "id");
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalse_whenIdTokenDiffersIncludingNullVsNonNull() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication nonNullId = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication differentId = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "other");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication nullId = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", null);

        assertNotEquals(nonNullId, differentId);
        assertNotEquals(nonNullId, nullId);

        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication nullId2 = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", null);
        assertEquals(nullId, nullId2, "Both idToken null and same accessToken -> equal");
    }

    @Test
    void equals_accountsForSuperFields_authenticatedFlagDiffers() {
        // Note: AbstractAuthenticationToken.equals() considers authentication state among other things.
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication b = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");

        // Same initial state => equal
        assertEquals(a, b);

        // Change authenticated on one => NOT equal
        a.setAuthenticated(true);
        assertNotEquals(a, b);

        // Align again => equal
        b.setAuthenticated(true);
        assertEquals(a, b);
    }

    @Test
    void equals_accountsForSuperFields_detailsDiffer() {
        // Note: AbstractAuthenticationToken.equals() includes details in equality.
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication b = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");

        // Baseline equal
        assertEquals(a, b);

        // Set details only in one instance => not equal
        a.setDetails("some-details");
        assertNotEquals(a, b);

        // Align details => equal again
        b.setDetails("some-details");
        assertEquals(a, b);
    }

    @Test
    void hashCode_changesWhenTokensOrSuperStateChange() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id");
        int base = a.hashCode();

        // Change tokens by creating a new instance with different values
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication differentTokens = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc2", "id");
        assertNotEquals(base, differentTokens.hashCode());

        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication differentId = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", "id2");
        assertNotEquals(base, differentId.hashCode());

        // Change super state (authenticated) should also affect hashCode
        a.setAuthenticated(true);
        int afterAuth = a.hashCode();
        assertNotEquals(base, afterAuth);

        // Change details should affect hashCode too
        a.setDetails("d1");
        int afterDetails = a.hashCode();
        assertNotEquals(afterAuth, afterDetails);
    }

    @Test
    void toString_includesClassNameAndAuthenticatedFlag() {
        es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication a = new es.altia.domeadapter.backend.shared.infrastructure.security.DualTokenAuthentication("acc", null);
        String s1 = a.toString();
        assertTrue(s1.contains("DualTokenAuthentication"), "toString should include simple class name");
        assertTrue(s1.contains("authenticated=false"), "toString should reflect unauthenticated state");

        a.setAuthenticated(true);
        String s2 = a.toString();
        assertTrue(s2.contains("authenticated=true"), "toString should reflect authenticated state");
    }
}
