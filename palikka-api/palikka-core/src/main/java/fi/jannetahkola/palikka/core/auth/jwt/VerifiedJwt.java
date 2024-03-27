package fi.jannetahkola.palikka.core.auth.jwt;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class VerifiedJwt {
    @NonNull
    PalikkaJwtType type;

    @NonNull
    JWSHeader header;

    @NonNull
    JWTClaimsSet claims;

    @NonNull
    String token;
}
