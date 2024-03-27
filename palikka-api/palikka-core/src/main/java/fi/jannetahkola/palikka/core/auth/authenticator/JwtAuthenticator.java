package fi.jannetahkola.palikka.core.auth.authenticator;

import fi.jannetahkola.palikka.core.auth.jwt.VerifiedJwt;

public interface JwtAuthenticator {
    void authenticate(VerifiedJwt verifiedJwt);
}
