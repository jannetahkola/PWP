package fi.jannetahkola.palikka.core.config.meta;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.AuthenticationConfig;
import fi.jannetahkola.palikka.core.config.JwtConfig;
import fi.jannetahkola.palikka.core.config.RedisConfig;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables {@link JwtService} and {@link PalikkaAuthenticationFilterConfigurer} for both producing and consuming JWTs,
 * depending on the configurations. Also enables Redis-based {@link RevokedTokenRepository}, which needs to be configured
 * via "palikka.redis".
 * <br><br>
 * This annotation also requires a bean for an implementation of {@link UsersClient}. Core provides
 * {@link EnableRemoteUsersIntegration} but you can also implement your own. Core's test JAR provides
 * helpers that enable an embedded Redis server.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({
        JwtConfig.class,
        RedisConfig.class,
        AuthenticationConfig.class
})
public @interface EnableAuthenticationSupport {
}
