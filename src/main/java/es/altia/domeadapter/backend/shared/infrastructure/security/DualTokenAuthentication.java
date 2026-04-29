package es.altia.domeadapter.shared.infrastructure.security;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Objects;

public final class DualTokenAuthentication extends AbstractAuthenticationToken {

    private final String accessToken;
    @Nullable private final String idToken;

    public DualTokenAuthentication(String accessToken, @Nullable String idToken) {
        super(null);
        this.accessToken = accessToken;
        this.idToken = idToken;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() { return accessToken; }

    @Override
    public Object getPrincipal() { return "N/A"; }

    @Nullable
    public String getIdToken() { return idToken; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DualTokenAuthentication that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(this.accessToken, that.accessToken)
                && Objects.equals(this.idToken, that.idToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessToken, idToken);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[authenticated=" + isAuthenticated() + "]";
    }
}
