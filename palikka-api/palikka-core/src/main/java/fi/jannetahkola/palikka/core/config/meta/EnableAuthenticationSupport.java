package fi.jannetahkola.palikka.core.config.meta;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.AuthenticationFilterConfig;
import fi.jannetahkola.palikka.core.config.JwtConfig;
import fi.jannetahkola.palikka.core.config.UsersIntegrationConfig;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables {@link JwtService} and {@link PalikkaAuthenticationFilterConfigurer}.
 * This annotation also requires a bean for an implementation of {@link UsersClient}.
 * Core provides {@link EnableRemoteUsersIntegration} but you can also implement your own.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({
        JwtConfig.class,
        AuthenticationFilterConfig.class
})
public @interface EnableAuthenticationSupport {
}
